package com.taskmanager.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.client.gemini.GeminiApiClient;
import com.taskmanager.dto.request.AiParseTaskRequest;
import com.taskmanager.dto.response.AiParseTaskResponse;
import com.taskmanager.entity.TimeBlock;
import com.taskmanager.repository.TimeBlockRepository;
import com.taskmanager.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final GeminiApiClient geminiApiClient;
    private final TimeBlockRepository timeBlockRepository;
    private final ObjectMapper objectMapper;

    public AiServiceImpl(GeminiApiClient geminiApiClient, TimeBlockRepository timeBlockRepository, ObjectMapper objectMapper) {
        this.geminiApiClient = geminiApiClient;
        this.timeBlockRepository = timeBlockRepository;
        this.objectMapper = objectMapper;
    }

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    // 1. Nhận diện từ ngữ thô tục / nói bậy / chửi bậy / xúc phạm
    private static final Pattern PROFANITY_PATTERN = Pattern.compile(
            "(?iU)\\b(" +
            "đụ|địt|dit|đm|dm|dcm|đcm|vcl|vcll|clgt|vl|đéo|deo|đếch|dech|đệch|" +
            "chó chết|cho chet|mẹ mày|me may|mẹ kiếp|me kiep|khốn nạn|khon nan|" +
            "cặc|cac|lồn|lon|buồi|buoi|dái|dai|dâm|dam|hãm lồn|ham lon|" +
            "bitch|fuck|fucking|shit|asshole|pussy|dick|bastard" +
            ")\\b"
    );

    // 2. Nhận diện câu chào hỏi / nói chơi / thử mic / small talk vô nghĩa
    private static final Pattern CASUAL_GREETING_PATTERN = Pattern.compile(
            "(?iU)^(?:alo+|a\\s*lô+|hello+|hi+|hey+|ê+|ơi+|ê\\s+ơi|chào|xin\\s+chào|chào\\s+bạn|chào\\s+em|chào\\s+bot|chào\\s+trợ\\s+lý|" +
            "test|thử|thử\\s+mic|thu\\s+mic|1\\s*2\\s*3|123|haha+|hihi+|hehe+|huhu+|" +
            "ừ|uk|uh|ok|oke|okie|được\\s+rồi|bạn\\s+là\\s+ai|bạn\\s+tên\\s+gì|có\\s+ai\\s+ở\\s+đây\\s+không)[\\s!?,.~-]*$"
    );

    /**
     * Kiểm tra nội dung câu lệnh: chặn nói bậy, câu chào hỏi, chuỗi vô nghĩa
     */
    private AiParseTaskResponse validatePromptContent(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return AiParseTaskResponse.builder()
                    .isValidTask(false)
                    .validationType("NONSENSE")
                    .validationMessage("Vui lòng nhập hoặc nói công việc bạn muốn lên lịch (ví dụ: 'Học tiếng Anh lúc 8h tối mai').")
                    .build();
        }

        String trimmed = prompt.trim();

        // Kiểm tra từ ngữ thô tục / nói bậy / chửi thề
        if (PROFANITY_PATTERN.matcher(trimmed).find()) {
            return AiParseTaskResponse.builder()
                    .isValidTask(false)
                    .validationType("INAPPROPRIATE")
                    .validationMessage("Nội dung chứa từ ngữ không phù hợp chuẩn mực để đưa vào lịch trình cá nhân. Vui lòng sử dụng ngôn từ lịch sự và nhập công việc bạn muốn thực hiện.")
                    .build();
        }

        // Kiểm tra câu chào hỏi / nói chơi / thử mic
        if (CASUAL_GREETING_PATTERN.matcher(trimmed).matches()) {
            return AiParseTaskResponse.builder()
                    .isValidTask(false)
                    .validationType("GREETING")
                    .validationMessage("Chào bạn! SmartTask AI đã sẵn sàng hỗ trợ. Hãy nhập hoặc nói cụ thể công việc cần lên lịch nhé (ví dụ: 'Học từ vựng tiếng Anh 8h tối mai' hoặc 'Chạy bộ 30 phút sáng mai').")
                    .build();
        }

        // Kiểm tra chuỗi vô nghĩa hoặc quá ngắn (dưới 3 ký tự không phải công việc)
        if (trimmed.length() < 3 || isGibberish(trimmed)) {
            return AiParseTaskResponse.builder()
                    .isValidTask(false)
                    .validationType("NONSENSE")
                    .validationMessage("Nội dung chưa đủ thông tin công việc để lên lịch. Bạn hãy mô tả rõ công việc cần làm (ví dụ: 'Họp nhóm đồ án lúc 9h sáng thứ 6').")
                    .build();
        }

        return null; // Nội dung hợp lệ
    }

    private boolean isGibberish(String text) {
        if (text.matches("^[\\s!?,.~@#$%^&*()_+=\\-\\[\\]{};:'\"/\\\\|<>]+$")) {
            return true;
        }
        String lower = text.toLowerCase();
        boolean hasVowel = lower.matches(".*[aeiouyáàảãạăắằẳẵặâấầẩẫậéèẻẽẹêếềểễệíìỉĩịóòỏõọôốồổỗộơớờởỡợúùủũụưứừửữựýỳỷỹỵ].*");
        if (!hasVowel && text.length() > 3) {
            return true;
        }
        return false;
    }

    @Override
    public AiParseTaskResponse parseTaskFromNaturalLanguage(AiParseTaskRequest request) {
        String prompt = request.getPrompt() != null ? request.getPrompt().trim() : "";
        Long userId = request.getUserId() != null ? request.getUserId() : 1L;

        // 0. Kiểm tra tính hợp lệ trước (Chặn nói bậy, nói chơi, câu chào, chuỗi vô nghĩa)
        AiParseTaskResponse preCheck = validatePromptContent(prompt);
        if (preCheck != null) {
            log.info("Prompt '{}' bị chặn: type={}, msg={}", prompt, preCheck.getValidationType(), preCheck.getValidationMessage());
            return preCheck;
        }

        // 1. Xác định thời điểm tham chiếu hiện tại
        OffsetDateTime refDateTime = parseReferenceDateTime(request.getCurrentDateTime());

        AiParseTaskResponse response = null;

        // 2. Thử gọi Google Gemini 1.5 Flash nếu client đã cấu hình
        if (geminiApiClient.isConfigured() && !prompt.isBlank()) {
            response = tryGeminiParse(prompt, refDateTime);
        }

        // 3. Fallback Heuristic Parser nếu Gemini chưa cấu hình hoặc gọi API thất bại
        if (response == null) {
            log.info("Sử dụng Heuristic Fallback Parser cho prompt: '{}'", prompt);
            response = fallbackHeuristicParse(prompt, refDateTime);
        }

        // Nếu response xác định không phải task hợp lệ thì trả về ngay (không kiểm tra xung đột)
        if (response != null && Boolean.FALSE.equals(response.getIsValidTask())) {
            return response;
        }

        // Đảm bảo tiêu đề luôn sạch, không chứa thứ/giờ/ngày tháng thừa & kiểm tra xung đột
        if (response != null) {
            if (response.getTasks() != null && !response.getTasks().isEmpty()) {
                for (AiParseTaskResponse t : response.getTasks()) {
                    if (t.getTitle() != null) {
                        t.setTitle(cleanTitle(t.getTitle()));
                    }
                    checkAndResolveConflicts(t, userId);
                }
                // Kiểm tra xung đột chéo giữa các công việc được bóc tách trong cùng câu lệnh
                if (response.getTasks().size() > 1) {
                    List<AiParseTaskResponse> tasks = response.getTasks();
                    for (int i = 0; i < tasks.size(); i++) {
                        AiParseTaskResponse t1 = tasks.get(i);
                        for (int j = i + 1; j < tasks.size(); j++) {
                            AiParseTaskResponse t2 = tasks.get(j);
                            if (t1.getStartTime() != null && t1.getEndTime() != null && t2.getStartTime() != null && t2.getEndTime() != null) {
                                try {
                                    OffsetDateTime s1 = OffsetDateTime.parse(t1.getStartTime());
                                    OffsetDateTime e1 = OffsetDateTime.parse(t1.getEndTime());
                                    OffsetDateTime s2 = OffsetDateTime.parse(t2.getStartTime());
                                    OffsetDateTime e2 = OffsetDateTime.parse(t2.getEndTime());
                                    if (s1.toInstant().isBefore(e2.toInstant()) && e1.toInstant().isAfter(s2.toInstant())) {
                                        t2.setHasConflict(true);
                                        t2.setConflictMessage("Trùng khung giờ với '" + t1.getTitle() + "' ("
                                                + s1.format(DateTimeFormatter.ofPattern("HH:mm")) + "–" + e1.format(DateTimeFormatter.ofPattern("HH:mm")) + ").");
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }
            } else if (response.getTitle() != null) {
                response.setTitle(cleanTitle(response.getTitle()));
                if ("Việc".equalsIgnoreCase(response.getTitle()) || "Công việc mới".equalsIgnoreCase(response.getTitle()) || response.getTitle().length() < 2) {
                    response.setIsValidTask(false);
                    response.setValidationType("GREETING");
                    response.setValidationMessage("SmartTask AI chưa nhận diện được công việc cụ thể. Hãy nhập tên việc bạn muốn làm (ví dụ: 'Học tiếng Anh lúc 8h tối mai').");
                    return response;
                }
                checkAndResolveConflicts(response, userId);
            }
        }

        return response;
    }

    /**
     * Gọi Gemini Flash bằng WebClient với JSON schema hỗ trợ 1 hoặc nhiều công việc
     */
    private AiParseTaskResponse tryGeminiParse(String prompt, OffsetDateTime refDateTime) {
        try {
            String dayOfWeekName = getVietnameseDayOfWeek(refDateTime.getDayOfWeek());
            String dateStr = refDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

            // Lấy danh sách các ngày trong tuần hiện tại để Gemini tham chiếu chính xác
            DayOfWeek currentDow = refDateTime.getDayOfWeek();
            String weekCalendar = buildWeekCalendarReference(refDateTime);

            String systemInstruction = String.format(
                    "Vai trò: Bạn là SmartTask Scheduler — trợ lý AI lập lịch công việc cá nhân thông minh hàng đầu cho người Việt Nam.\n"
                            + "\n"
                            + "═══ NGỮ CẢNH THỜI GIAN HIỆN TẠI ═══\n"
                            + "• Bây giờ: %s, ngày %s (UTC+07:00, Asia/Ho_Chi_Minh)\n"
                            + "• Thứ hiện tại: %s (ISO DayOfWeek = %d, trong đó 1=Thứ Hai, 7=Chủ Nhật)\n"
                            + "• Bản đồ tuần hiện tại:\n%s\n"
                            + "\n"
                            + "═══ NHIỆM VỤ QUAN TRỌNG NHẤT ═══\n"
                            + "Phân tích câu lệnh tiếng Việt tự nhiên của người dùng:\n"
                            + "1) Câu lệnh có thể chứa 1 HOẶC NHIỀU CÔNG VIỆC CẦN LÊN LỊCH.\n"
                            + "   Ví dụ: 'Hội họp bạn bè 8h sáng nay rồi đi 9h đi chợ' chứa 2 việc: 1) 'Hội họp bạn bè' lúc 8h, 2) 'Đi chợ' lúc 9h.\n"
                            + "   Ví dụ: 'Sáng mai 7h chạy bộ 45 phút, 9h họp team, chiều 14h gặp khách' chứa 3 việc: 1) 'Chạy bộ' 7h, 2) 'Họp team' 9h, 3) 'Gặp khách hàng' 14h.\n"
                            + "2) BẠN PHẢI NHẬN DIỆN, BÓC TÁCH VÀ PHÂN CHIA ĐẦY ĐỦ TẤT CẢ CÁC CÔNG VIỆC ĐÓ thành từng phần tử trong mảng `tasks`.\n"
                            + "3) Nếu câu lệnh chỉ có 1 công việc, mảng `tasks` sẽ có 1 phần tử.\n"
                            + "4) Nếu câu lệnh là câu chào ('alo alo', 'xin chào'), nói bậy, nói chơi vô nghĩa → đặt `isValidTask: false`, `tasks: []`.\n"
                            + "\n"
                            + "═══ QUY TẮC CHO TỪNG CÔNG VIỆC TRONG `tasks` ═══\n"
                            + "▸ title (string, BẮT BUỘC): Tiêu đề ngắn gọn, bản chất công việc. Viết hoa chữ cái đầu. TUYỆT ĐỐI KHÔNG chứa: ngày/tháng, thứ, giờ, thời lượng, độ ưu tiên.\n"
                            + "▸ categoryType (string, BẮT BUỘC): chỉ 1 trong 4: 'WORK' | 'STUDY' | 'HEALTH' | 'ENTERTAINMENT'. (Ví dụ: 'Đi chợ' -> WORK, 'Hội họp bạn bè' -> ENTERTAINMENT, 'Chạy bộ' -> HEALTH, 'Học bài' -> STUDY)\n"
                            + "▸ startTime (string ISO-8601, BẮT BUỘC): YYYY-MM-DDTHH:mm:ss+07:00 theo đúng ngày và giờ suy luận từ câu lệnh.\n"
                            + "▸ estimatedDurationMinutes (integer, BẮT BUỘC): Số phút ước tính. Nếu không nói rõ: HEALTH->45, STUDY->90, WORK->60, ENTERTAINMENT->60. Nếu việc sau bắt đầu lúc X và việc trước lúc Y, thời lượng việc trước có thể tính bằng X - Y.\n"
                            + "▸ priority (string, BẮT BUỘC): 'URGENT' | 'HIGH' | 'MEDIUM' | 'LOW'.\n"
                            + "▸ isFixedTime (boolean): true nếu có giờ chỉ định cụ thể, false nếu linh hoạt.\n"
                            + "▸ note (string): 1 câu giải thích ngắn gọn ngữ cảnh và thời gian.\n"
                            + "\n"
                            + "═══ ĐẦU RA JSON BẮT BUỘC (KHÔNG MARKDOWN) ═══\n"
                            + "Trả về 1 JSON object chuẩn gồm các khóa: isValidTask (boolean), validationMessage (string), tasks (array các object công việc như trên).",
                    dayOfWeekName, dateStr, dayOfWeekName, currentDow.getValue(), weekCalendar);

            Optional<String> geminiResult = geminiApiClient.generateJsonResponse(systemInstruction, prompt);
            if (geminiResult.isPresent()) {
                String jsonText = geminiResult.get().trim();
                if (jsonText.startsWith("```json")) {
                    jsonText = jsonText.substring(7);
                } else if (jsonText.startsWith("```")) {
                    jsonText = jsonText.substring(3);
                }
                if (jsonText.endsWith("```")) {
                    jsonText = jsonText.substring(0, jsonText.length() - 3);
                }
                jsonText = jsonText.trim();

                JsonNode node = objectMapper.readTree(jsonText);
                boolean isValidTask = node.has("isValidTask") ? node.path("isValidTask").asBoolean(true) : true;
                String validationMsg = node.path("validationMessage").asText(null);

                if (!isValidTask) {
                    return AiParseTaskResponse.builder()
                            .isValidTask(false)
                            .validationType("GREETING")
                            .validationMessage(validationMsg != null && !validationMsg.isBlank()
                                    ? validationMsg
                                    : "Chào bạn! Hãy nhập cụ thể công việc bạn muốn lên lịch nhé (ví dụ: 'Học tiếng Anh lúc 8h tối mai').")
                            .build();
                }

                List<AiParseTaskResponse> taskList = new ArrayList<>();
                JsonNode tasksNode = node.path("tasks");
                if (tasksNode.isArray() && !tasksNode.isEmpty()) {
                    for (JsonNode tNode : tasksNode) {
                        String tTitle = cleanTitle(tNode.path("title").asText(""));
                        if (tTitle.length() < 2 || "Việc".equalsIgnoreCase(tTitle) || "Công việc mới".equalsIgnoreCase(tTitle)
                                || "Alo".equalsIgnoreCase(tTitle) || "Alo alo".equalsIgnoreCase(tTitle)) {
                            continue;
                        }
                        String catType = tNode.path("categoryType").asText("WORK");
                        String startStr = tNode.path("startTime").asText();
                        int duration = tNode.path("estimatedDurationMinutes").asInt(60);
                        String priority = tNode.path("priority").asText("MEDIUM");
                        boolean isFixed = tNode.path("isFixedTime").asBoolean(true);
                        String note = tNode.path("note").asText("Được phân tích tự động bởi Gemini AI");

                        OffsetDateTime startTime = parseIsoDateTimeOrFallback(startStr,
                                refDateTime.plusDays(1).withHour(9).withMinute(0));
                        OffsetDateTime endTime = startTime.plusMinutes(duration);

                        taskList.add(AiParseTaskResponse.builder()
                                .isValidTask(true)
                                .title(tTitle)
                                .categoryType(catType)
                                .startTime(startTime.format(ISO_FORMATTER))
                                .endTime(endTime.format(ISO_FORMATTER))
                                .estimatedDurationMinutes(duration)
                                .priority(priority)
                                .isFixedTime(isFixed)
                                .formattedDateLabel(formatDateLabel(startTime, endTime))
                                .note(note)
                                .hasConflict(false)
                                .build());
                    }
                } else if (node.has("title")) {
                    String tTitle = cleanTitle(node.path("title").asText(""));
                    if (tTitle.length() >= 2 && !"Việc".equalsIgnoreCase(tTitle) && !"Công việc mới".equalsIgnoreCase(tTitle)
                            && !"Alo".equalsIgnoreCase(tTitle)) {
                        String catType = node.path("categoryType").asText("WORK");
                        String startStr = node.path("startTime").asText();
                        int duration = node.path("estimatedDurationMinutes").asInt(60);
                        String priority = node.path("priority").asText("MEDIUM");
                        boolean isFixed = node.path("isFixedTime").asBoolean(true);
                        String note = node.path("note").asText("Được phân tích tự động bởi Gemini AI");

                        OffsetDateTime startTime = parseIsoDateTimeOrFallback(startStr,
                                refDateTime.plusDays(1).withHour(9).withMinute(0));
                        OffsetDateTime endTime = startTime.plusMinutes(duration);

                        taskList.add(AiParseTaskResponse.builder()
                                .isValidTask(true)
                                .title(tTitle)
                                .categoryType(catType)
                                .startTime(startTime.format(ISO_FORMATTER))
                                .endTime(endTime.format(ISO_FORMATTER))
                                .estimatedDurationMinutes(duration)
                                .priority(priority)
                                .isFixedTime(isFixed)
                                .formattedDateLabel(formatDateLabel(startTime, endTime))
                                .note(note)
                                .hasConflict(false)
                                .build());
                    }
                }

                if (taskList.isEmpty()) {
                    return AiParseTaskResponse.builder()
                            .isValidTask(false)
                            .validationType("GREETING")
                            .validationMessage("SmartTask AI chưa nhận diện được công việc cụ thể. Hãy nhập tên việc cần làm nhé.")
                            .build();
                }

                AiParseTaskResponse primary = taskList.get(0);
                return AiParseTaskResponse.builder()
                        .isValidTask(true)
                        .tasks(taskList)
                        .title(primary.getTitle())
                        .categoryType(primary.getCategoryType())
                        .startTime(primary.getStartTime())
                        .endTime(primary.getEndTime())
                        .estimatedDurationMinutes(primary.getEstimatedDurationMinutes())
                        .priority(primary.getPriority())
                        .isFixedTime(primary.getIsFixedTime())
                        .formattedDateLabel(primary.getFormattedDateLabel())
                        .note(primary.getNote())
                        .hasConflict(false)
                        .build();
            }
        } catch (Exception e) {
            log.warn("Gemini parse failed: {}. Sẽ chuyển sang Heuristic.", e.getMessage());
        }
        return null;
    }

    /**
     * Thuật toán Heuristic nội bộ (Rule-based & Regex) khi không có mạng hoặc Gemini key chưa nhập
     */
    private AiParseTaskResponse fallbackHeuristicParse(String prompt, OffsetDateTime refDateTime) {
        // Tách câu nếu có nhiều mệnh đề liên kết bằng: rồi đi, rồi, sau đó, tiếp theo, xong rồi, xong, ;, \n
        String[] clauses = prompt.split("(?i)\\s+(?:rồi đi|roi di|rồi|roi|sau đó|sau do|tiếp theo|tiep theo|xong rồi|xong roi|xong)\\s+|[;\\n]+");
        if (clauses.length > 1) {
            List<AiParseTaskResponse> multi = new ArrayList<>();
            for (String clause : clauses) {
                String trimmed = clause.trim();
                if (trimmed.length() >= 3) {
                    AiParseTaskResponse res = parseSingleHeuristic(trimmed, refDateTime);
                    if (res != null && Boolean.TRUE.equals(res.getIsValidTask())
                            && res.getTitle() != null && !"Việc".equalsIgnoreCase(res.getTitle())
                            && !"Công việc mới".equalsIgnoreCase(res.getTitle())) {
                        multi.add(res);
                    }
                }
            }
            if (multi.size() > 1) {
                AiParseTaskResponse primary = multi.get(0);
                return AiParseTaskResponse.builder()
                        .isValidTask(true)
                        .tasks(multi)
                        .title(primary.getTitle())
                        .categoryType(primary.getCategoryType())
                        .startTime(primary.getStartTime())
                        .endTime(primary.getEndTime())
                        .estimatedDurationMinutes(primary.getEstimatedDurationMinutes())
                        .priority(primary.getPriority())
                        .isFixedTime(primary.getIsFixedTime())
                        .formattedDateLabel(primary.getFormattedDateLabel())
                        .note(primary.getNote())
                        .hasConflict(false)
                        .build();
            }
        }
        return parseSingleHeuristic(prompt, refDateTime);
    }

    private AiParseTaskResponse parseSingleHeuristic(String prompt, OffsetDateTime refDateTime) {
        String normalized = prompt;
        // Chuẩn hóa từ số nói "một" -> "1"
        normalized = normalized.replaceAll("(?i)\\b(?:một|mốt)\\s*(?:giờ|gio|h)\\b", "1h");
        normalized = normalized.replaceAll("(?i)\\b(?:một|mốt)\\s*(?:tiếng|tieng)\\b", "1 tiếng");
        normalized = normalized.replaceAll("(?i)\\b(?:một|mốt)\\s*(?:lịch|lich)\\b", "1 lịch");
        normalized = normalized.replaceAll("(?i)\\b(?:một|mốt)\\s*(?:ngày|ngay)\\b", "1 ngày");
        normalized = normalized.replaceAll("(?i)\\b(?:một|mốt)\\s*(?:phút|phut|p)\\b", "1 phút");
        normalized = normalized.replaceAll("(?i)\\b(?:vào lúc|lúc|từ|đến|tới|khoảng|tầm)\\s+(?:một|mốt)\\b", "$0 1");
        normalized = normalized.replaceAll("(?i)\\b(?:một|mốt)\\b", "1");

        // Chuẩn hóa các số khác trong ngữ cảnh thời gian:
        normalized = normalized.replaceAll("(?i)\\bhai\\s*(?:giờ|gio|h)\\b", "2h");
        normalized = normalized.replaceAll("(?i)\\bhai\\s*(?:tiếng|tieng)\\b", "2 tiếng");
        normalized = normalized.replaceAll("(?i)\\bba\\s*(?:giờ|gio|h)\\b", "3h");
        normalized = normalized.replaceAll("(?i)\\bbốn\\s*(?:giờ|gio|h)\\b", "4h");
        normalized = normalized.replaceAll("(?i)\\bnăm\\s*(?:giờ|gio|h)\\b", "5h");
        normalized = normalized.replaceAll("(?i)\\bsáu\\s*(?:giờ|gio|h)\\b", "6h");
        normalized = normalized.replaceAll("(?i)\\bbảy\\s*(?:giờ|gio|h)\\b", "7h");
        normalized = normalized.replaceAll("(?i)\\btám\\s*(?:giờ|gio|h)\\b", "8h");
        normalized = normalized.replaceAll("(?i)\\bchín\\s*(?:giờ|gio|h)\\b", "9h");
        normalized = normalized.replaceAll("(?i)\\bmười\\s*(?:giờ|gio|h)\\b", "10h");

        // Chuẩn hóa cách viết liền/rời thời lượng và giờ phút:
        normalized = normalized.replaceAll("(?i)\\b(\\d+)tiếng\\b", "$1 tiếng");
        normalized = normalized.replaceAll("(?i)\\b(\\d+)h(\\d{1,2})p(?:h|hút)?\\b", "$1h$2");

        String lower = normalized.toLowerCase();

        // 1. Phân loại Category
        String categoryType = "WORK";
        if (lower.contains("học") || lower.contains("hoc")
                || lower.contains("tiếng anh") || lower.contains("tieng anh")
                || lower.contains("anh văn") || lower.contains("anh van")
                || lower.contains("english") || lower.contains("ielts") || lower.contains("toeic") || lower.contains("toefl")
                || lower.contains("toán") || lower.contains("toan")
                || lower.contains("văn") || lower.contains("van")
                || lower.contains("lý") || lower.contains("ly")
                || lower.contains("hóa") || lower.contains("hoa")
                || lower.contains("sinh học") || lower.contains("ngoại ngữ")
                || lower.contains("đồ án") || lower.contains("do an")
                || lower.contains("bài tập") || lower.contains("bai tap") || lower.contains("bài") || lower.contains("bai")
                || lower.contains("thi") || lower.contains("ôn") || lower.contains("on")
                || lower.contains("lập trình") || lower.contains("lap trinh")
                || lower.contains("nhập môn") || lower.contains("nhap mon")
                || lower.contains("kỹ thuật") || lower.contains("ky thuat")
                || lower.contains("môn") || lower.contains("mon")
                || lower.contains("sách") || lower.contains("sach")) {
            categoryType = "STUDY";
        } else if (lower.contains("tập") || lower.contains("tap")
                || lower.contains("gym") || lower.contains("chạy bộ") || lower.contains("chay bo")
                || lower.contains("thể dục") || lower.contains("the duc")
                || lower.contains("thuốc") || lower.contains("thuoc") || lower.contains("khám") || lower.contains("kham")) {
            categoryType = "HEALTH";
        } else if (lower.contains("chơi") || lower.contains("choi")
                || lower.contains("phim") || lower.contains("game")
                || lower.contains("cà phê") || lower.contains("ca phe") || lower.contains("cafe")
                || lower.contains("nghỉ") || lower.contains("nghi")) {
            categoryType = "ENTERTAINMENT";
        }

        // 2. Độ ưu tiên
        String priority = "MEDIUM";
        if (lower.contains("khẩn cấp") || lower.contains("gấp")) {
            priority = "URGENT";
        } else if (lower.contains("ưu tiên cao") || lower.contains("quan trọng") || lower.contains("rất quan trọng")) {
            priority = "HIGH";
        } else if (lower.contains("ưu tiên thấp") || lower.contains("thong thả") || lower.contains("khi nào rảnh")) {
            priority = "LOW";
        }

        // 3. Ngày tháng (Date & Day of Week)
        OffsetDateTime targetDate = refDateTime;

        // Bóc tách thứ trong tuần: thứ 2 -> thứ 7, chủ nhật / cn
        Integer targetDayOfWeek = null; // 1 = Monday, ..., 7 = Sunday
        if (Pattern.compile("\\b((?:thứ|thu)\\s*(?:hai|2)|t2)\\b").matcher(lower).find()) {
            targetDayOfWeek = DayOfWeek.MONDAY.getValue();
        } else if (Pattern.compile("\\b((?:thứ|thu)\\s*(?:ba|3)|t3)\\b").matcher(lower).find()) {
            targetDayOfWeek = DayOfWeek.TUESDAY.getValue();
        } else if (Pattern.compile("\\b((?:thứ|thu)\\s*(?:tư|tu|bốn|bon|4)|t4)\\b").matcher(lower).find()) {
            targetDayOfWeek = DayOfWeek.WEDNESDAY.getValue();
        } else if (Pattern.compile("\\b((?:thứ|thu)\\s*(?:năm|nam|5)|t5)\\b").matcher(lower).find()) {
            targetDayOfWeek = DayOfWeek.THURSDAY.getValue();
        } else if (Pattern.compile("\\b((?:thứ|thu)\\s*(?:sáu|sau|6)|t6)\\b").matcher(lower).find()) {
            targetDayOfWeek = DayOfWeek.FRIDAY.getValue();
        } else if (Pattern.compile("\\b((?:thứ|thu)\\s*(?:bảy|bay|7)|t7)\\b").matcher(lower).find()) {
            targetDayOfWeek = DayOfWeek.SATURDAY.getValue();
        } else if (Pattern.compile("\\b((?:chủ|chu)\\s*(?:nhật|nhat)|cn)\\b").matcher(lower).find()) {
            targetDayOfWeek = DayOfWeek.SUNDAY.getValue();
        }

        // 3.1 Bóc tách ngày cụ thể: ngày 17, ngày 17/10, ngày 17 tháng 10
        Pattern exactDatePattern = Pattern.compile("(?:vào\\s*)?ngày\\s*(\\d{1,2})(?:(?:/|\\s*tháng\\s*)(\\d{1,2}))?(?:(?:/|\\s*năm\\s*)(\\d{4}))?");
        Matcher exactDateMatcher = exactDatePattern.matcher(lower);

        if (exactDateMatcher.find()) {
            int d = Integer.parseInt(exactDateMatcher.group(1));
            int m = exactDateMatcher.group(2) != null ? Integer.parseInt(exactDateMatcher.group(2)) : refDateTime.getMonthValue();
            int y = exactDateMatcher.group(3) != null ? Integer.parseInt(exactDateMatcher.group(3)) : refDateTime.getYear();
            try {
                targetDate = targetDate.withYear(y).withMonth(m).withDayOfMonth(d);
            } catch (Exception ignored) {}
        } else if (targetDayOfWeek != null) {
            int currentDow = refDateTime.getDayOfWeek().getValue(); // 1 = Monday, ..., 7 = Sunday
            int dayDiff = targetDayOfWeek - currentDow;
            // CẢI TIẾN: Nếu thứ đã qua trong tuần hiện tại (dayDiff <= 0), tự động chuyển sang tuần tiếp theo
            if (dayDiff <= 0) {
                dayDiff += 7;
            }
            if (lower.contains("tuần sau")) {
                dayDiff += 7;
            }
            targetDate = refDateTime.plusDays(dayDiff);
        } else if (lower.contains("ngày hôm sau") || lower.contains("hôm sau")) {
            targetDate = refDateTime.plusDays(1);
        } else if (lower.contains("ngày mai") || lower.contains("chiều mai") || lower.contains("sáng mai")
                || lower.contains("tối mai")) {
            targetDate = refDateTime.plusDays(1);
        } else if (lower.contains("ngày mốt") || lower.contains("hôm kia")) {
            targetDate = refDateTime.plusDays(2);
        } else if (lower.contains("tuần sau")) {
            targetDate = refDateTime.plusDays(7);
        }

        // 4. Giờ giấc & Khoảng thời gian (Start Hour, End Hour, Duration)
        int targetHour = 15;
        int targetMinute = 0;
        int duration = 60;
        boolean hasExplicitHour = false;

        // Ưu tiên 1: Nhận diện khoảng giờ dạng "vào lúc 20 giờ đến 21 giờ", "1h đến 2h30p", "7h to 8h", "từ 7h đến 8h", "7h - 8h"
        Pattern rangePattern = Pattern.compile(
                "(?:(?:từ|tu|vào lúc|vao luc|lúc|luc|vào|vao|tầm|tam|khoảng|khoang)\\s*)?(\\d{1,2})(?:\\s*(?:h|giờ|gio)(?:\\s*(\\d{1,2})(?:p|ph|phút|phut)?)?|:(\\d{2}))?\\s*(?:to|đến|den|tới|toi|-)\\s*(?:(?:lúc|luc|vào|vao)\\s*)?(\\d{1,2})(?:\\s*(?:h|giờ|gio)(?:\\s*(\\d{1,2})(?:p|ph|phút|phut)?)?|:(\\d{2}))?");
        Matcher rangeMatcher = rangePattern.matcher(lower);

        if (rangeMatcher.find()) {
            hasExplicitHour = true;
            int startH = Integer.parseInt(rangeMatcher.group(1));
            int startM = 0;
            if (rangeMatcher.group(2) != null) {
                startM = Integer.parseInt(rangeMatcher.group(2));
            } else if (rangeMatcher.group(3) != null) {
                startM = Integer.parseInt(rangeMatcher.group(3));
            }

            int endH = Integer.parseInt(rangeMatcher.group(4));
            int endM = 0;
            if (rangeMatcher.group(5) != null) {
                endM = Integer.parseInt(rangeMatcher.group(5));
            } else if (rangeMatcher.group(6) != null) {
                endM = Integer.parseInt(rangeMatcher.group(6));
            }

            // Xử lý phân biệt sáng / chiều / tối
            if ((lower.contains("tối") || lower.contains("toi") || lower.contains("chiều") || lower.contains("chieu")) && startH < 12) {
                startH += 12;
                if (endH < 12)
                    endH += 12;
            } else if (startH >= 1 && startH <= 6 && !lower.contains("sáng") && !lower.contains("sang") && !lower.contains("am")) {
                // CẢI TIẾN: Giờ 1-6 không nói sáng/chiều thì mặc định hiểu là chiều/tối
                startH += 12;
                if (endH < 12) endH += 12;
            }

            targetHour = startH;
            targetMinute = startM;
            int totalStartMin = startH * 60 + startM;
            int totalEndMin = endH * 60 + endM;
            if (totalEndMin > totalStartMin) {
                duration = totalEndMin - totalStartMin;
            }
        } else {
            // Ưu tiên 2: Bóc tách thời lượng riêng nếu không có range
            Pattern durMinutePattern = Pattern.compile("(?:trong|khoảng|khoang|kéo dài|keo dai|tầm|tam)?\\s*(\\d+)\\s*(phút|phut|p)\\b");
            Matcher durMinuteMatcher = durMinutePattern.matcher(lower);
            if (durMinuteMatcher.find()) {
                duration = Integer.parseInt(durMinuteMatcher.group(1));
            } else if (lower.contains("tiếng rưỡi") || lower.contains("tieng ruoi") || lower.contains("1h30")) {
                duration = 90;
            } else if (lower.contains("nửa tiếng") || lower.contains("nua tieng") || lower.contains("30 phút") || lower.contains("30 phut")) {
                duration = 30;
            } else {
                Pattern durHourPattern = Pattern
                        .compile("(?:trong|khoảng|khoang|kéo dài|keo dai|tầm|tam)\\s*(\\d+)\\s*(?:tiếng|tieng|giờ|gio|h)\\b");
                Matcher durHourMatcher = durHourPattern.matcher(lower);
                if (durHourMatcher.find()) {
                    duration = Integer.parseInt(durHourMatcher.group(1)) * 60;
                } else {
                    Pattern standaloneHourPattern = Pattern.compile("(\\d+)\\s*(?:tiếng|tieng|giờ|gio)\\b");
                    Matcher standaloneHourMatcher = standaloneHourPattern.matcher(lower);
                    if (standaloneHourMatcher.find()) {
                        duration = Integer.parseInt(standaloneHourMatcher.group(1)) * 60;
                    }
                }
            }

            // Bóc tách mốc giờ bắt đầu đơn lẻ: "lúc 15h", "vào 7h", "15h", "vào lúc 20 giờ"
            String timeSearchText = lower.replaceAll("(?i)\\blần\\s*(?:thứ\\s*)?\\d+\\b", "");

            Pattern hourPattern = Pattern.compile(
                    "(?:(?:vào lúc|vao luc|lúc|luc|vào|vao|tầm|tam|khoảng|khoang)\\s+(\\d{1,2})(?:\\s*(?:h|giờ|gio)(?:\\s*(\\d{1,2}))?|:(\\d{2}))?|\\b(\\d{1,2})(?:\\s*(?:h|giờ|gio)(?:\\s*(\\d{1,2}))?|:(\\d{2})))(?:\\s*(?:sáng|sang|chiều|chieu|tối|toi))?");
            Matcher hourMatcher = hourPattern.matcher(timeSearchText);
            if (hourMatcher.find()) {
                hasExplicitHour = true;
                String matchedHourStr = hourMatcher.group(1) != null ? hourMatcher.group(1) : hourMatcher.group(4);
                targetHour = Integer.parseInt(matchedHourStr);
                String minuteStr = hourMatcher.group(2) != null ? hourMatcher.group(2) : (hourMatcher.group(3) != null ? hourMatcher.group(3) : (hourMatcher.group(5) != null ? hourMatcher.group(5) : hourMatcher.group(6)));
                if (minuteStr != null) {
                    try {
                        targetMinute = Integer.parseInt(minuteStr);
                    } catch (Exception ignored) {}
                }
                if ((lower.contains("chiều") || lower.contains("chieu") || lower.contains("tối") || lower.contains("toi")) && targetHour < 12) {
                    targetHour += 12;
                } else if (targetHour >= 1 && targetHour <= 6 && !lower.contains("sáng") && !lower.contains("sang") && !lower.contains("am")) {
                    // CẢI TIẾN: Giờ 1-6 tự động chuyển thành chiều nếu không có từ 'sáng'
                    targetHour += 12;
                }
            } else if (lower.contains("sáng") || lower.contains("sang")) {
                // Buổi sáng: phân loại thông minh
                if ("HEALTH".equals(categoryType)) {
                    targetHour = 6;
                    targetMinute = 30; // Thể dục / chạy bộ sáng sớm
                } else if ("URGENT".equals(priority)) {
                    targetHour = 8;
                    targetMinute = 0; // Việc khẩn cấp xử lý ngay đầu ngày
                } else if ("STUDY".equals(categoryType) || "WORK".equals(categoryType)) {
                    targetHour = 8;
                    targetMinute = 30; // Giờ vàng làm việc & học tập
                } else {
                    targetHour = 9;
                    targetMinute = 0;
                }
            } else if (lower.contains("chiều") || lower.contains("chieu")) {
                // Buổi chiều: phân loại thông minh
                if ("HEALTH".equals(categoryType)) {
                    targetHour = 17;
                    targetMinute = 30; // Tập gym, thể thao cuối chiều
                } else if ("URGENT".equals(priority)) {
                    targetHour = 13;
                    targetMinute = 30; // Việc gấp làm ngay đầu giờ chiều
                } else if ("ENTERTAINMENT".equals(categoryType)) {
                    targetHour = 16;
                    targetMinute = 0; // Giải trí, cà phê xả hơi
                } else {
                    targetHour = 14;
                    targetMinute = 0; // Công việc / học tập buổi chiều
                }
            } else if (lower.contains("tối") || lower.contains("toi")) {
                // Buổi tối: phân loại thông minh
                if ("ENTERTAINMENT".equals(categoryType)) {
                    targetHour = 20;
                    targetMinute = 0; // Xem phim, chơi game, thư giãn
                } else if ("STUDY".equals(categoryType)) {
                    targetHour = 19;
                    targetMinute = 30; // Tự học buổi tối
                } else {
                    targetHour = 19;
                    targetMinute = 0;
                }
            } else {
                // Không chỉ định mốc buổi: TỰ ĐỘNG LẬP LỊCH THÔNG MINH theo độ ưu tiên & thể loại
                if ("URGENT".equals(priority)) {
                    targetHour = 8;
                    targetMinute = 0; // KHẨN CẤP: Đặt ngay đầu ngày
                } else if ("HIGH".equals(priority)) {
                    targetHour = 9;
                    targetMinute = 0; // ƯU TIÊN CAO: Khung giờ vàng năng suất cao
                } else if ("HEALTH".equals(categoryType)) {
                    targetHour = 17;
                    targetMinute = 30; // SỨC KHỎE: Cuối chiều 17:30
                } else if ("ENTERTAINMENT".equals(categoryType)) {
                    targetHour = 19;
                    targetMinute = 30; // GIẢI TRÍ: Buổi tối 19:30
                } else if ("STUDY".equals(categoryType)) {
                    targetHour = 8;
                    targetMinute = 30; // HỌC TẬP: Buổi sáng 08:30
                } else if ("LOW".equals(priority)) {
                    targetHour = 16;
                    targetMinute = 0; // ƯU TIÊN THẤP: Cuối giờ chiều
                } else {
                    targetHour = 9;
                    targetMinute = 30; // CÔNG VIỆC TIÊU CHUẨN: 09:30
                }
            }
        }

        OffsetDateTime startTime = targetDate.withHour(Math.min(targetHour, 23)).withMinute(targetMinute).withSecond(0)
                .withNano(0);
        OffsetDateTime endTime = startTime.plusMinutes(duration);

        // 5. Làm sạch Tiêu đề hoàn toàn (Loại bỏ thứ, dải giờ, thời lượng, mốc giờ khỏi tên task)
        String cleanTitle = cleanTitle(normalized);

        if (cleanTitle == null || cleanTitle.isBlank() || "Việc".equalsIgnoreCase(cleanTitle)
                || "Công việc mới".equalsIgnoreCase(cleanTitle) || "Alo".equalsIgnoreCase(cleanTitle)
                || "Alo alo".equalsIgnoreCase(cleanTitle) || cleanTitle.length() < 2) {
            return AiParseTaskResponse.builder()
                    .isValidTask(false)
                    .validationType("GREETING")
                    .validationMessage("SmartTask AI chưa nhận diện được công việc cụ thể. Bạn hãy nhập hoặc nói tên việc cần làm nhé (ví dụ: 'Học từ vựng tiếng Anh 8h tối mai' hoặc 'Chạy bộ 30 phút').")
                    .build();
        }

        return AiParseTaskResponse.builder()
                .isValidTask(true)
                .title(cleanTitle)
                .categoryType(categoryType)
                .startTime(startTime.format(ISO_FORMATTER))
                .endTime(endTime.format(ISO_FORMATTER))
                .estimatedDurationMinutes(duration)
                .priority(priority)
                .isFixedTime(hasExplicitHour)
                .formattedDateLabel(formatDateLabel(startTime, endTime))
                .note("Phân tích tự động bởi SmartTask AI")
                .hasConflict(false)
                .build();
    }

    /**
     * Làm sạch hoàn toàn tiêu đề công việc, loại bỏ các tiền tố mệnh lệnh và cụm ngày/giờ/thứ thừa
     */
    private String cleanTitle(String text) {
        if (text == null || text.isBlank())
            return "Công việc mới";
        String clean = text;

        // 0. Chuẩn hóa từ số nói "một" -> "1"
        clean = clean.replaceAll("(?iU)\\b(?:một|mốt)\\b", "1");

        // 1. Loại bỏ các tiền tố mệnh lệnh
        clean = clean.replaceAll(
                "(?iU)^(?:hãy|hay)?\\s*(?:lặp|lap|lập|tao|tạo|lên|len|đặt|dat|xếp|xep|nhắc|nhac|sắp xếp|sap xep)\\s*(?:cho tôi|cho toi|cho em|cho mình|cho minh|tôi|toi|giúp tôi|giup toi)?\\s*(?:một|mot|1)?\\s*(?:lịch trình|lich trinh|công việc|cong viec|tác vụ|tac vu|lịch|lich|buổi|buoi)?\\s*",
                "");
        clean = clean.replaceAll("(?iU)^(nhắc tôi|nhac toi|nhắc em|nhac em|nhắc mình|nhac minh|hãy nhớ|hay nho|nhớ|nho|lên lịch|len lich|tạo lịch|tao lich|đặt lịch|dat lich|sắp xếp cho tôi|sap xep cho toi|sắp xếp|sap xep|làm ơn|lam on)\\s*", "");

        // 1.1 Loại bỏ số rác đứng trước "vào lúc", "lúc", "từ" (trừ trường hợp thứ 2, thứ 3... thứ 7)
        clean = clean.replaceAll("(?iU)(?<!(?:thứ|thu)\\s*)\\b\\d+\\s+(?=(?:vào lúc|vao luc|lúc|luc|từ|tu|đến|den|tới|toi|khoảng|khoang|tầm|tam)\\b)", "");
        clean = clean.replaceAll("(?iU)\\blần\\s*(?:thứ\\s*)?\\d+\\b", "");

        // 2. Loại bỏ dải giờ
        clean = clean.replaceAll(
                "(?iU)(?:(?:từ|tu|vào lúc|vao luc|lúc|luc|tầm|tam|khoảng|khoang)\\s*)?\\d{1,2}(?:\\s*(?:h|giờ|gio)(?:\\s*\\d{1,2}(?:p|ph|phút|phut)?)?|:\\d{2})?\\s*(?:to|đến|den|tới|toi|-)\\s*(?:(?:lúc|luc|vào|vao)\\s*)?\\d{1,2}(?:\\s*(?:h|giờ|gio)(?:\\s*\\d{1,2}(?:p|ph|phút|phut)?)?|:\\d{2})?(?:\\s*(?:sáng|sang|chiều|chieu|tối|toi))?",
                "");

        // 3. Loại bỏ mốc giờ đơn lẻ
        clean = clean.replaceAll(
                "(?iU)\\b(?:vào lúc|vao luc|lúc|luc|vào|vao|tầm|tam|khoảng|khoang)\\s*\\d{1,2}(?:\\s*(?:h|giờ|gio)(?:\\s*\\d{1,2}(?:p|ph|phút|phut)?)?|:\\d{2})?(?:\\s*(?:sáng|sang|chiều|chieu|tối|toi))?\\b",
                "");
        clean = clean.replaceAll("(?iU)\\b\\d{1,2}\\s*(?:h|giờ|gio)(?:\\s*\\d{1,2}(?:p|ph|phút|phut)?)?\\b", "");

        // 4. Loại bỏ thời lượng
        clean = clean.replaceAll("(?iU)\\b(?:trong|khoảng|khoang|kéo dài|keo dai|tầm|tam)?\\s*\\d+\\s*(?:phút|phut|p|tiếng|tieng|giờ|gio)(?:\\s*(?:rưỡi|ruoi|30))?\\b", "");
        clean = clean.replaceAll("(?iU)\\b(?:khoảng|khoang|tầm|tam)?\\s*(?:\\d+\\s*)?(?:tiếng rưỡi|tieng ruoi|nửa tiếng|nua tieng)\\b", "");
        clean = clean.replaceAll("(?iU)\\b(?:khoảng|khoang|tầm|tam)\\s+(?:rưỡi|ruoi)\\b", "");
        clean = clean.replaceAll("(?iU)\\b(?:rưỡi|ruoi)\\b", "");

        // 5. Loại bỏ ngày cụ thể & từ chỉ ngày (chỉ xóa khi đứng với cấu trúc thời gian)
        clean = clean.replaceAll("(?iU)\\b(?:vào\\s*|vao\\s*)?(?:ngày|ngay)\\s*\\d{1,2}(?:[/\\-]\\d{1,2})?(?:[/\\-]\\d{4})?\\b", "");
        clean = clean.replaceAll("(?iU)\\b(?:vào\\s*|vao\\s*)?(?:ngày|ngay)\\s*\\d{1,2}\\s*(?:tháng|thang)\\s*\\d{1,2}\\b", "");
        clean = clean.replaceAll("(?iU)\\b(ngày hôm sau|ngay hom sau|hôm sau|hom sau)\\b", "");

        // 6. Loại bỏ thứ trong tuần
        clean = clean.replaceAll(
                "(?iU)(?:\\b|(?<=\\s|^))((?:thứ|thu)\\s*(?:hai|ba|tư|tu|bốn|bon|năm|nam|sáu|sau|bảy|bay|[2-7])|(?:chủ|chu)\\s*(?:nhật|nhat)|cn|t[2-7])(?:\\b|(?=\\s|$))",
                "");
        clean = clean.replaceAll("(?iU)\\b(?:thứ|thu)\\b", "");

        // 7. Loại bỏ từ chỉ mốc thời gian tương đối & buổi
        clean = clean.replaceAll(
                "(?iU)\\b(hôm nay|hom nay|hôm qua|hom qua|ngày mai|ngay mai|ngày mốt|ngay mot|ngày kia|ngay kia|hôm kia|hom kia|tuần này|tuan nay|tuần sau|tuan sau|tháng này|thang nay|tháng sau|thang sau)\\b",
                "");
        clean = clean.replaceAll(
                "(?iU)\\b(sáng nay|sang nay|chiều nay|chieu nay|tối nay|toi nay|sáng mai|sang mai|chiều mai|chieu mai|tối mai|toi mai|sáng|sang|chiều|chieu|tối|toi|trưa|trua|đêm|dem)\\b",
                "");

        // 8. Loại bỏ độ ưu tiên
        clean = clean.replaceAll("(?iU)\\b(ưu tiên|uu tien)\\s*(?:cao|thấp|thap|trung bình|trung binh|khẩn cấp|khan cap)\\b", "");
        clean = clean.replaceAll("(?iU)\\b(khẩn cấp|khan cap|quan trọng|quan trong|rất quan trọng|rat quan trong|gấp|gap)\\b", "");

        // 9. Dọn sạch các từ nối còn sót và dấu câu thừa
        clean = clean.replaceAll("(?iU)\\b(vào lúc|vao luc|vào|vao|lúc|luc|từ|tu|đến|den|tới|toi|khoảng|khoang|tầm|tam)\\b", "");
        clean = clean.replaceAll("[\\.,;:!\\?~\\-_]+$", "").trim();
        clean = clean.replaceAll("^[\\.,;:!\\?~\\-_]+", "").trim();
        clean = clean.replaceAll("\\s+", " ").trim();

        if (clean.length() > 1) {
            clean = clean.substring(0, 1).toUpperCase() + clean.substring(1);
        } else if (clean.isEmpty()) {
            clean = "Công việc mới";
        }
        return clean;
    }

    /**
     * Kiểm tra xung đột với các TimeBlock đã có trong DB và đề xuất giờ trống tiếp theo
     */
    private void checkAndResolveConflicts(AiParseTaskResponse response, Long userId) {
        if (response == null || response.getStartTime() == null || userId == null)
            return;

        try {
            OffsetDateTime start = OffsetDateTime.parse(response.getStartTime());
            OffsetDateTime end = OffsetDateTime.parse(response.getEndTime());

            // Lấy các timeblocks trong khoảng +- 2 ngày
            OffsetDateTime windowStart = start.minusDays(2);
            OffsetDateTime windowEnd = end.plusDays(2);

            List<TimeBlock> existingBlocks = timeBlockRepository.findByUserIdAndStartTimeBetween(userId, windowStart,
                    windowEnd);

            if (existingBlocks != null && !existingBlocks.isEmpty()) {
                existingBlocks.sort(Comparator.comparing(TimeBlock::getStartTime));

                for (TimeBlock b : existingBlocks) {
                    if (b.getStartTime() != null && b.getEndTime() != null) {
                        // CẢI TIẾN: Dùng toInstant() để so sánh chuẩn xác tuyệt đối bất kể offset
                        if (start.toInstant().isBefore(b.getEndTime().toInstant()) 
                                && end.toInstant().isAfter(b.getStartTime().toInstant())) {
                            response.setHasConflict(true);
                            response.setConflictingTaskTitle(b.getTitle());

                            // Chuyển đổi múi giờ sang GMT+7 chuẩn để hiển thị cho người dùng
                            OffsetDateTime bStartVn = b.getStartTime().atZoneSameInstant(VIETNAM_ZONE).toOffsetDateTime();
                            OffsetDateTime bEndVn = b.getEndTime().atZoneSameInstant(VIETNAM_ZONE).toOffsetDateTime();

                            String bStartStr = bStartVn.format(DateTimeFormatter.ofPattern("HH:mm"));
                            String bEndStr = bEndVn.format(DateTimeFormatter.ofPattern("HH:mm"));

                            int dur = response.getEstimatedDurationMinutes() != null
                                    ? response.getEstimatedDurationMinutes()
                                    : 60;

                            // Tìm khung giờ trống tiếp theo hợp lệ (tránh các task khác và tránh giờ nghỉ trưa)
                            OffsetDateTime altStart = findNextAvailableFreeSlot(existingBlocks, bEndVn.plusMinutes(15), dur);
                            OffsetDateTime altEnd = altStart.plusMinutes(dur);

                            String altStartStr = altStart.format(DateTimeFormatter.ofPattern("HH:mm"));
                            String altEndStr = altEnd.format(DateTimeFormatter.ofPattern("HH:mm"));

                            response.setConflictMessage(String.format(
                                    "Trùng với %s (%s–%s). Hệ thống đề xuất dời sang giờ trống: %s–%s.",
                                    b.getTitle(), bStartStr, bEndStr, altStartStr, altEndStr));

                            response.setSuggestedAlternativeStartTime(altStart.format(ISO_FORMATTER));
                            response.setSuggestedAlternativeEndTime(altEnd.format(ISO_FORMATTER));
                            break;
                        }
                    }
                }
            }

            // Kiểm tra gợi ý cảnh báo thời gian đặc biệt (sớm trước 6h sáng hoặc sau 22h khuya)
            OffsetDateTime startVn = start.atZoneSameInstant(VIETNAM_ZONE).toOffsetDateTime();
            OffsetDateTime endVn = end.atZoneSameInstant(VIETNAM_ZONE).toOffsetDateTime();
            int endH = endVn.getHour();
            int endM = endVn.getMinute();
            int startH = startVn.getHour();

            String warningNote = null;
            if (startH < 5) {
                warningNote = "Lịch diễn ra vào sáng rất sớm (trước 05:00 sáng).";
            } else if (endH > 22 || (endH == 22 && endM > 30)) {
                warningNote = "Lịch kết thúc khá muộn (sau 22:00 đêm). Bạn nhớ dành thời gian nghỉ ngơi nhé.";
            }

            if (warningNote != null) {
                if (response.getNote() == null || response.getNote().isBlank()) {
                    response.setNote(warningNote);
                } else if (!response.getNote().contains(warningNote)) {
                    response.setNote(response.getNote() + " · " + warningNote);
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi kiểm tra xung đột lịch: {}", e.getMessage());
        }
    }

    /**
     * Tìm khung giờ trống kế tiếp trong ngày làm việc (07:00 - 20:00, tránh nghỉ trưa 12:00 - 13:00)
     */
    private OffsetDateTime findNextAvailableFreeSlot(List<TimeBlock> existingBlocks, OffsetDateTime candidateStart, int durationMinutes) {
        OffsetDateTime candidate = candidateStart.atZoneSameInstant(VIETNAM_ZONE).toOffsetDateTime();

        // Đảm bảo không rơi vào giờ nghỉ trưa (12:00 - 13:00)
        int h = candidate.getHour();
        if (h >= 12 && h < 13) {
            candidate = candidate.withHour(13).withMinute(0);
        }

        // Lặp kiểm tra xem candidate có bị trùng với block nào khác không
        boolean hasConflict = true;
        int maxAttempts = 15;
        while (hasConflict && maxAttempts-- > 0) {
            hasConflict = false;
            OffsetDateTime candidateEnd = candidate.plusMinutes(durationMinutes);

            // Nếu vượt quá 20:00, chuyển sang sáng hôm sau lúc 08:00
            if (candidateEnd.getHour() > 20 || (candidateEnd.getHour() == 20 && candidateEnd.getMinute() > 0)) {
                candidate = candidate.plusDays(1).withHour(8).withMinute(0);
                candidateEnd = candidate.plusMinutes(durationMinutes);
            }

            // Nếu rơi vào nghỉ trưa 12:00 - 13:00
            if ((candidate.getHour() < 13 && candidateEnd.getHour() > 12) || (candidate.getHour() == 12)) {
                candidate = candidate.withHour(13).withMinute(0);
                candidateEnd = candidate.plusMinutes(durationMinutes);
            }

            if (existingBlocks != null) {
                for (TimeBlock b : existingBlocks) {
                    if (b.getStartTime() != null && b.getEndTime() != null) {
                        OffsetDateTime bStartVn = b.getStartTime().atZoneSameInstant(VIETNAM_ZONE).toOffsetDateTime();
                        OffsetDateTime bEndVn = b.getEndTime().atZoneSameInstant(VIETNAM_ZONE).toOffsetDateTime();

                        if (candidate.isBefore(bEndVn) && candidateEnd.isAfter(bStartVn)) {
                            // Bị trùng! Dời candidate sang sau khi block b kết thúc + 15 phút đệm
                            candidate = bEndVn.plusMinutes(15);
                            hasConflict = true;
                            break;
                        }
                    }
                }
            }
        }

        return candidate;
    }

    private OffsetDateTime parseReferenceDateTime(String clientDateTime) {
        if (clientDateTime != null && !clientDateTime.isBlank()) {
            String trimmed = clientDateTime.trim();
            try {
                return OffsetDateTime.parse(trimmed);
            } catch (Exception ignored) {
            }
            try {
                return LocalDateTime.parse(trimmed).atZone(VIETNAM_ZONE).toOffsetDateTime();
            } catch (Exception ignored) {
            }
            try {
                return Instant.parse(trimmed).atZone(VIETNAM_ZONE).toOffsetDateTime();
            } catch (Exception ignored) {
            }
        }
        return OffsetDateTime.now(VIETNAM_ZONE);
    }

    private OffsetDateTime parseIsoDateTimeOrFallback(String str, OffsetDateTime fallback) {
        if (str != null && !str.isBlank()) {
            String trimmed = str.trim();
            try {
                return OffsetDateTime.parse(trimmed);
            } catch (Exception ignored) {
            }
            try {
                return LocalDateTime.parse(trimmed).atZone(VIETNAM_ZONE).toOffsetDateTime();
            } catch (Exception ignored) {
            }
            try {
                return Instant.parse(trimmed).atZone(VIETNAM_ZONE).toOffsetDateTime();
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    private String formatDateLabel(OffsetDateTime start, OffsetDateTime end) {
        String dayOfWeek = getVietnameseDayOfWeek(start.getDayOfWeek());
        String date = start.format(DateTimeFormatter.ofPattern("dd/MM"));
        String startH = start.format(DateTimeFormatter.ofPattern("HH:mm"));
        String endH = end.format(DateTimeFormatter.ofPattern("HH:mm"));
        return String.format("%s, %s · %s–%s", dayOfWeek, date, startH, endH);
    }

    private String getVietnameseDayOfWeek(DayOfWeek day) {
        switch (day) {
            case MONDAY:
                return "Thứ Hai";
            case TUESDAY:
                return "Thứ Ba";
            case WEDNESDAY:
                return "Thứ Tư";
            case THURSDAY:
                return "Thứ Năm";
            case FRIDAY:
                return "Thứ Sáu";
            case SATURDAY:
                return "Thứ Bảy";
            case SUNDAY:
                return "Chủ Nhật";
            default:
                return "Hôm nay";
        }
    }

    private String buildWeekCalendarReference(OffsetDateTime refDateTime) {
        StringBuilder sb = new StringBuilder();
        int currentDowVal = refDateTime.getDayOfWeek().getValue(); // 1 = Monday ... 7 = Sunday
        OffsetDateTime monday = refDateTime.minusDays(currentDowVal - 1);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter isoDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < 7; i++) {
            OffsetDateTime day = monday.plusDays(i);
            DayOfWeek dow = day.getDayOfWeek();
            String dowVn = getVietnameseDayOfWeek(dow);
            String status;
            if (i + 1 < currentDowVal) {
                status = "[ĐÃ QUA trong tuần này]";
            } else if (i + 1 == currentDowVal) {
                status = "[HÔM NAY]";
            } else {
                status = "[SẮP TỚI trong tuần này]";
            }
            sb.append(String.format("   • %s (%s): %s %s\n",
                    dowVn, dtf.format(day), isoDate.format(day), status));
        }
        return sb.toString().trim();
    }
}
