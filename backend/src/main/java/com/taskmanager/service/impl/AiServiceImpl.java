package com.taskmanager.service.impl;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import com.taskmanager.client.gemini.GeminiApiClient;
import com.taskmanager.dto.AutoScheduleDto;
import com.taskmanager.dto.SubtaskDto;
import com.taskmanager.dto.request.AiParseTaskRequest;
import com.taskmanager.dto.response.AiParseTaskResponse;
import com.taskmanager.entity.CaiDatNguoiDung;
import com.taskmanager.entity.CongViec;
import com.taskmanager.entity.CongViecCon;
import com.taskmanager.entity.KhoiThoiGian;
import com.taskmanager.entity.NhatKyAi;
import com.taskmanager.repository.*;
import com.taskmanager.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final GeminiApiClient geminiApiClient;
    private final TimeBlockRepository timeBlockRepository;
    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final SubtaskRepository subtaskRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final AiSchedulingLogRepository aiSchedulingLogRepository;
    private final JsonMapper objectMapper;

    public AiServiceImpl(GeminiApiClient geminiApiClient,
                         TimeBlockRepository timeBlockRepository,
                         TaskRepository taskRepository,
                         CategoryRepository categoryRepository,
                         SubtaskRepository subtaskRepository,
                         UserPreferenceRepository userPreferenceRepository,
                         AiSchedulingLogRepository aiSchedulingLogRepository,
                         JsonMapper objectMapper) {
        this.geminiApiClient = geminiApiClient;
        this.timeBlockRepository = timeBlockRepository;
        this.taskRepository = taskRepository;
        this.categoryRepository = categoryRepository;
        this.subtaskRepository = subtaskRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.aiSchedulingLogRepository = aiSchedulingLogRepository;
        this.objectMapper = objectMapper;
    }

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final int MAX_AI_TASKS = 50;
    private static final int MAX_AI_SUBTASKS = 20;

    private record ScheduleSlot(OffsetDateTime start, OffsetDateTime end) {}
    private record SlotChoice(int slotIndex, OffsetDateTime start) {}

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

        // Kiểm tra từ ngữ thô tục / nói bậy / chửi thề (Lớp bảo vệ trực tiếp để phản hồi siêu tốc)
        if (PROFANITY_PATTERN.matcher(trimmed).find()) {
            return AiParseTaskResponse.builder()
                    .isValidTask(false)
                    .validationType("INAPPROPRIATE")
                    .validationMessage("Nội dung chứa từ ngữ không phù hợp chuẩn mực để đưa vào lịch trình cá nhân. Vui lòng sử dụng ngôn từ lịch sự và nhập công việc bạn muốn thực hiện.")
                    .build();
        }

        // Không chặn các câu chào hỏi/nói chuyện ("Hello", "Bạn khỏe không",...) ở đây
        // để chuyển tiếp đến Gemini phân tích và trả lời tự nhiên theo đúng ngữ cảnh!

        // Kiểm tra chuỗi toàn ký tự đặc biệt vô nghĩa
        if (isGibberish(trimmed)) {
            return AiParseTaskResponse.builder()
                    .isValidTask(false)
                    .validationType("CHAT")
                    .validationMessage("Chào bạn! SmartTask AI đã sẵn sàng hỗ trợ. Hãy nhập hoặc nói cụ thể công việc cần lên lịch nhé (ví dụ: 'Học tiếng Anh lúc 8h tối mai').")
                    .build();
        }

        return null; // Tiếp tục gửi Gemini hoặc bộ bóc tách heuristic
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
        Long userId = request.getUserId();
        if (userId == null) {
            throw new IllegalArgumentException("Yêu cầu cần phiên đăng nhập hợp lệ.");
        }
        if (prompt.length() > 4000) {
            throw new IllegalArgumentException("Nội dung yêu cầu AI không được vượt quá 4.000 ký tự.");
        }

        // 0. Kiểm tra tính hợp lệ trước (Chặn nói bậy, nói chơi, câu chào, chuỗi vô nghĩa)
        AiParseTaskResponse preCheck = validatePromptContent(prompt);
        if (preCheck != null) {
            log.info("Yêu cầu AI bị chặn: type={}", preCheck.getValidationType());
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
            log.info("Sử dụng Heuristic Fallback Parser cho yêu cầu AI.");
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
                            + "═══ PHÂN LOẠI CÂU LỆNH & QUYẾT ĐỊNH CỜ FLAG (BẮT BUỘC) ═══\n"
                            + "Bạn hãy phân tích câu lệnh của người dùng và quyết định cờ flag `validationType`:\n"
                            + "1) 'INAPPROPRIATE': Nếu câu lệnh chứa từ ngữ thô tục, chửi thề, tục tĩu, lăng mạ, khiếm nhã. Đặt `isValidTask: false`, `validationType: 'INAPPROPRIATE'`, `tasks: []`, `validationMessage: 'Nội dung chứa từ ngữ không phù hợp chuẩn mực để đưa vào lịch trình cá nhân. Vui lòng sử dụng ngôn từ lịch sự và nhập công việc bạn muốn thực hiện.'`.\n"
                            + "2) 'CHAT': Nếu câu lệnh là câu chào ('Hello', 'Xin chào', 'Hi', 'Alo'), hỏi thăm ('Bạn khỏe không?', 'Hôm nay thế nào?'), nói chuyện phiếm hoặc câu hỏi không liên quan đến lên lịch ('Thời tiết hôm nay', 'Bạn tên là gì', 'Bạn làm được gì', 'Cảm ơn nhé', v.v.). Đặt `isValidTask: false`, `validationType: 'CHAT'`, `tasks: []`. Trong `validationMessage`: Trả lời tự nhiên, thân thiện theo ngữ cảnh.\n"
                            + "3) 'VALID': Nếu câu lệnh chứa 1 hoặc nhiều công việc cụ thể cần lên lịch. Đặt `isValidTask: true`, `validationType: 'VALID'`, `validationMessage: null`, `tasks: [danh sách các công việc]`.\n"
                            + "\n"
                            + "═══ NGUYÊN TẮC BÓC TÁCH MỘT LÈO NHIỀU CÔNG VIỆC (CỰC KỲ QUAN TRỌNG) ═══\n"
                            + "Khi người dùng nói hoặc gõ MỘT LÈO NHIỀU CÔNG VIỆC TRONG CÙNG MỘT CÂU (ngăn cách bằng dấu phẩy ',', từ 'và', 'rồi', 'sau đó', 'tiếp theo', 'xong rồi', 'xong thì', 'chiều', 'tối', hoặc liệt kê các mốc giờ 8h..., 10h..., 15h...):\n"
                            + "• TUYỆT ĐỐI KHÔNG ĐƯỢC GỘP TẤT CẢ THÀNH 1 CÔNG VIỆC DUY NHẤT!\n"
                            + "• BẮT BUỘC PHẢI TÁCH RỜI TỪNG CÔNG VIỆC THÀNH TỪNG PHẦN TỬ RIÊNG BIỆT TRONG MẢNG `tasks`. Bao nhiêu công việc người dùng kể thì trả về bấy nhiêu khung tương ứng!\n"
                            + "• Kế thừa ngày: Nếu đầu câu nói 'Sáng mai...' rồi kể 1 lèo các việc thì TOÀN BỘ các việc sau đó đều kế thừa ngày MAI (trừ khi có việc nói rõ ngày khác).\n"
                            + "• Tiêu đề từng việc: Chỉ lấy phần hành động bản chất (Ví dụ: 'Học tiếng Anh', 'Họp nhóm đồ án', 'Đi bơi', 'Tập gym'). Tuyệt đối không chứa ngày, thứ, mốc giờ trong tiêu đề.\n"
                            + "\n"
                            + "═══ VÍ DỤ BÓC TÁCH NHIỀU VIỆC CHUẨN XÁC (FEW-SHOT EXAMPLES) ═══\n"
                            + "• Ví dụ 1: Người dùng nói: 'Sáng mai 8h học Tiếng Anh 1 tiếng, sau đó 10h họp nhóm đồ án 2 tiếng, chiều 15h đi bơi'\n"
                            + "  -> tasks gồm 3 phần tử riêng biệt:\n"
                            + "     1. title: 'Học Tiếng Anh', categoryType: 'STUDY', startTime: '<ngày_mai>T08:00:00+07:00', estimatedDurationMinutes: 60, priority: 'MEDIUM'\n"
                            + "     2. title: 'Họp nhóm đồ án', categoryType: 'WORK', startTime: '<ngày_mai>T10:00:00+07:00', estimatedDurationMinutes: 120, priority: 'HIGH'\n"
                            + "     3. title: 'Đi bơi', categoryType: 'HEALTH', startTime: '<ngày_mai>T15:00:00+07:00', estimatedDurationMinutes: 60, priority: 'MEDIUM'\n"
                            + "• Ví dụ 2: Người dùng nói: 'Hôm nay 9h làm báo cáo, chiều 2h gặp khách hàng, 5h chiều tập gym, tối 8h xem phim'\n"
                            + "  -> tasks gồm ĐỦ 4 phần tử riêng biệt cho từng khung giờ 09:00, 14:00, 17:00, 20:00.\n"
                            + "• Ví dụ 3: Người dùng nói: 'Mai 8h học bài rồi 10h đi đá bóng'\n"
                            + "  -> tasks gồm ĐỦ 2 phần tử riêng biệt cho 08:00 và 10:00 ngày mai.\n"
                            + "\n"
                            + "═══ CẤU TRÚC CHO MỖI CÔNG VIỆC TRONG `tasks` ═══\n"
                            + "   ▸ title (string, BẮT BUỘC): Tiêu đề ngắn gọn, bản chất công việc. Viết hoa chữ cái đầu. TUYỆT ĐỐI KHÔNG chứa: ngày/tháng, thứ, giờ, thời lượng.\n"
                            + "   ▸ categoryType (string, BẮT BUỘC): chỉ 1 trong 4: 'WORK' | 'STUDY' | 'HEALTH' | 'ENTERTAINMENT'.\n"
                            + "   ▸ startTime (string ISO-8601, BẮT BUỘC): YYYY-MM-DDTHH:mm:ss+07:00 theo đúng ngày và giờ suy luận từ câu lệnh.\n"
                            + "   ▸ estimatedDurationMinutes (integer, BẮT BUỘC): Số phút ước tính. Nếu không nói rõ: HEALTH->45, STUDY->90, WORK->60, ENTERTAINMENT->60.\n"
                            + "   ▸ priority (string, BẮT BUỘC): 'URGENT' | 'HIGH' | 'MEDIUM' | 'LOW'.\n"
                            + "   ▸ isFixedTime (boolean): true nếu có giờ chỉ định cụ thể, false nếu linh hoạt.\n"
                            + "   ▸ note (string): 1 câu giải thích ngắn gọn ngữ cảnh và thời gian.\n"
                            + "\n"
                            + "═══ ĐẦU RA JSON BẮT BUỘC (KHÔNG MARKDOWN) ═══\n"
                            + "{\n"
                            + "  \"isValidTask\": boolean,\n"
                            + "  \"validationType\": \"VALID\" | \"INAPPROPRIATE\" | \"CHAT\",\n"
                            + "  \"validationMessage\": string hoặc null,\n"
                            + "  \"tasks\": [ ... ]\n"
                            + "}",
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
                String valType = node.path("validationType").asString("").trim().toUpperCase();
                String validationMsg = node.path("validationMessage").asString(null);

                if (!isValidTask || "INAPPROPRIATE".equals(valType) || "CHAT".equals(valType) || "GREETING".equals(valType)) {
                    String finalValType = "INAPPROPRIATE".equals(valType) ? "INAPPROPRIATE" : "CHAT";
                    return AiParseTaskResponse.builder()
                            .isValidTask(false)
                            .validationType(finalValType)
                            .validationMessage(validationMsg != null && !validationMsg.isBlank()
                                    ? validationMsg
                                    : ("INAPPROPRIATE".equals(finalValType)
                                            ? "Nội dung chứa từ ngữ không phù hợp chuẩn mực để đưa vào lịch trình cá nhân. Vui lòng sử dụng ngôn từ lịch sự và nhập công việc bạn muốn thực hiện."
                                            : "Chào bạn! SmartTask AI rất vui được hỗ trợ bạn. Bạn có công việc gì cần lên lịch hôm nay không?"))
                            .build();
                }

                List<AiParseTaskResponse> taskList = new ArrayList<>();
                JsonNode tasksNode = node.path("tasks");
                if (tasksNode.isArray() && !tasksNode.isEmpty()) {
                    int taskLimit = Math.min(tasksNode.size(), MAX_AI_TASKS);
                    for (int taskIndex = 0; taskIndex < taskLimit; taskIndex++) {
                        JsonNode tNode = tasksNode.get(taskIndex);
                        String tTitle = cleanTitle(tNode.path("title").asString(""));
                        if (tTitle.length() < 2 || tTitle.length() > 255 || "Việc".equalsIgnoreCase(tTitle) || "Công việc mới".equalsIgnoreCase(tTitle)
                                || "Alo".equalsIgnoreCase(tTitle) || "Alo alo".equalsIgnoreCase(tTitle)) {
                            continue;
                        }
                        String catType = normalizeAiCategory(tNode.path("categoryType").asString("WORK"));
                        String startStr = tNode.path("startTime").asString();
                        int duration = tNode.path("estimatedDurationMinutes").asInt(60);
                        if (duration < 1 || duration > 1440) {
                            continue;
                        }
                        String priority = normalizeAiPriority(tNode.path("priority").asString("MEDIUM"));
                        boolean isFixed = tNode.path("isFixedTime").asBoolean(true);
                        String note = limitText(tNode.path("note").asString("Được phân tích tự động bởi Gemini AI"), 1000);

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
                    String tTitle = cleanTitle(node.path("title").asString(""));
                    if (tTitle.length() >= 2 && tTitle.length() <= 255 && !"Việc".equalsIgnoreCase(tTitle) && !"Công việc mới".equalsIgnoreCase(tTitle)
                            && !"Alo".equalsIgnoreCase(tTitle)) {
                        String catType = normalizeAiCategory(node.path("categoryType").asString("WORK"));
                        String startStr = node.path("startTime").asString();
                        int duration = node.path("estimatedDurationMinutes").asInt(60);
                        if (duration < 1 || duration > 1440) {
                            return null;
                        }
                        String priority = normalizeAiPriority(node.path("priority").asString("MEDIUM"));
                        boolean isFixed = node.path("isFixedTime").asBoolean(true);
                        String note = limitText(node.path("note").asString("Được phân tích tự động bởi Gemini AI"), 1000);

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
            log.warn("Gemini parse failed ({}). Sẽ chuyển sang Heuristic.", e.getClass().getSimpleName());
        }
        return null;
    }

    /**
     * Thuật toán Heuristic nội bộ (Rule-based & Regex) khi không có mạng hoặc Gemini key chưa nhập
     */
    private AiParseTaskResponse fallbackHeuristicParse(String prompt, OffsetDateTime refDateTime) {
        List<String> clauses = splitMultiTaskPrompt(prompt);
        if (clauses.size() > 1) {
            List<AiParseTaskResponse> multi = new ArrayList<>();
            OffsetDateTime inheritedDate = null;
            for (String clause : clauses) {
                String trimmed = clause.trim();
                if (trimmed.length() >= 3) {
                    AiParseTaskResponse res = parseSingleHeuristic(trimmed, refDateTime, inheritedDate);
                    if (res != null && Boolean.TRUE.equals(res.getIsValidTask())
                            && res.getTitle() != null && !"Việc".equalsIgnoreCase(res.getTitle())
                            && !"Công việc mới".equalsIgnoreCase(res.getTitle())) {
                        multi.add(res);
                        if (inheritedDate == null && res.getStartTime() != null) {
                            try {
                                inheritedDate = OffsetDateTime.parse(res.getStartTime());
                            } catch (Exception ignored) {}
                        }
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
        return parseSingleHeuristic(prompt, refDateTime, null);
    }

    /**
     * Tách thông minh câu lệnh chứa một lèo nhiều công việc
     */
    private List<String> splitMultiTaskPrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) return List.of();
        String text = prompt.trim();

        // Loại bỏ phần mở đầu như "Mai có 3 việc:", "Tôi có các việc sau:"
        text = text.replaceAll("(?iU)^(?:hôm nay|ngày mai|mai)?\\s*(?:tôi có|mình có|em có|có|cần làm)?\\s*\\d*\\s*(?:công việc|việc|tác vụ|kế hoạch|lịch trình)?\\s*(?:sau|cần làm|cần xếp|như sau)?\\s*[:;-]\\s*", "");

        // Tách theo dấu chấm phẩy, xuống dòng, từ nối, dấu phẩy/và trước mốc thời gian
        String splitRegex = "(?iU)(?:\\s*;\\s*|\\r?\\n+|\\s+(?:rồi đi|roi di|sau đó|sau do|tiếp theo|tiep theo|tiếp đến|tiep den|kế tiếp|ke tiep|xong rồi|xong roi|xong thì|xong thi)\\s+|\\s*,\\s*(?=(?:sau đó|tiếp|xong|rồi|lúc|từ|vào|sáng|trưa|chiều|tối|đêm|mai|hôm nay|thứ|thu|chủ nhật|cn|\\d{1,2}(?:h|:\\d{2}|\\s*giờ)))|\\s+(?:và|va|với|voi)\\s+(?=(?:sau đó|tiếp|xong|rồi|lúc|từ|vào|sáng|trưa|chiều|tối|đêm|mai|hôm nay|thứ|thu|chủ nhật|cn|\\d{1,2}(?:h|:\\d{2}|\\s*giờ)))|\\s*(?:^|\\s+)(?:\\d+[\\.\\)-]|việc\\s*\\d+\\s*[:;-]?)\\s*)";

        String[] rawClauses = text.split(splitRegex);
        List<String> result = new ArrayList<>();
        for (String c : rawClauses) {
            String trimmed = c.trim();
            if (trimmed.length() >= 3) {
                // Tách thêm nếu bên trong clause còn chứa "rồi" hoặc "xong" trước giờ
                String[] subClauses = trimmed.split("(?iU)\\s+(?:rồi|roi|xong)\\s+(?=\\d{1,2}(?:h|:\\d{2}|\\s*giờ)|lúc|vào|chiều|sáng|tối)");
                for (String sc : subClauses) {
                    String scTrimmed = sc.trim();
                    if (scTrimmed.length() >= 3) {
                        result.add(scTrimmed);
                    }
                }
            }
        }
        return result.isEmpty() ? List.of(text) : result;
    }

    private AiParseTaskResponse parseSingleHeuristic(String prompt, OffsetDateTime refDateTime, OffsetDateTime inheritedTargetDate) {
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

        // Kiểm tra câu chào hỏi / trò chuyện thông thường trong fallback heuristic
        if (CASUAL_GREETING_PATTERN.matcher(prompt.trim()).matches()
                || lower.matches("^(?:hello|hi|xin chào|chào|bạn khỏe không|ban khoe khong|bạn tên gì|alo|alo alo)[\\s!?,.~-]*$")) {
            String chatReply = "Chào bạn! SmartTask AI rất vui được hỗ trợ bạn. Bạn có công việc gì cần mình xếp lịch hôm nay không?";
            if (lower.contains("khỏe") || lower.contains("khoe")) {
                chatReply = "Mình là trợ lý ảo nên luôn tràn đầy năng lượng để đồng hành cùng bạn nè! Hôm nay bạn có kế hoạch hay công việc gì cần xếp lịch không?";
            } else if (lower.contains("bạn là ai") || lower.contains("ban la ai") || lower.contains("bạn tên gì") || lower.contains("ban ten gi")) {
                chatReply = "Mình là SmartTask AI — trợ lý thông minh giúp bạn phân tích và xếp lịch trình cá nhân tối ưu nhất! Bạn muốn thêm công việc gì nào?";
            }
            return AiParseTaskResponse.builder()
                    .isValidTask(false)
                    .validationType("CHAT")
                    .validationMessage(chatReply)
                    .build();
        }

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
        OffsetDateTime targetDate = inheritedTargetDate != null ? inheritedTargetDate : refDateTime;

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
                || lower.contains("tối mai") || Pattern.compile("\\bmai\\b").matcher(lower).find()) {
            targetDate = refDateTime.plusDays(1);
        } else if (lower.contains("ngày mốt") || lower.contains("hôm kia")) {
            targetDate = refDateTime.plusDays(2);
        } else if (lower.contains("tuần sau")) {
            targetDate = refDateTime.plusDays(7);
        } else if (lower.contains("hôm nay") || lower.contains("sáng nay") || lower.contains("chiều nay") || lower.contains("tối nay")) {
            targetDate = refDateTime;
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
                "(?iU)\\b(hôm nay|hom nay|hôm qua|hom qua|ngày mai|ngay mai|ngày mốt|ngay mot|ngày kia|ngay kia|hôm kia|hom kia|tuần này|tuan nay|tuần sau|tuan sau|tháng này|thang nay|tháng sau|thang sau|mai)\\b",
                "");
        clean = clean.replaceAll(
                "(?iU)\\b(sáng nay|sang nay|chiều nay|chieu nay|tối nay|toi nay|sáng mai|sang mai|chiều mai|chieu mai|tối mai|toi mai|sáng|sang|chiều|chieu|tối|toi|trưa|trua|đêm|dem)\\b",
                "");

        // 8. Loại bỏ độ ưu tiên
        clean = clean.replaceAll("(?iU)\\b(ưu tiên|uu tien)\\s*(?:cao|thấp|thap|trung bình|trung binh|khẩn cấp|khan cap)\\b", "");
        clean = clean.replaceAll("(?iU)\\b(khẩn cấp|khan cap|quan trọng|quan trong|rất quan trọng|rat quan trong|gấp|gap)\\b", "");

        // 9. Dọn sạch các từ nối còn sót và dấu câu thừa
        clean = clean.replaceAll("(?iU)\\b(vào lúc|vao luc|vào|vao|lúc|luc|từ|tu|đến|den|tới|toi|khoảng|khoang|tầm|tam)\\b", "");
        clean = clean.replaceAll("(?iU)^(?:sau đó|sau do|tiếp theo|tiep theo|tiếp đến|tiep den|kế tiếp|ke tiep|xong rồi|xong roi|xong thì|xong thi|rồi|roi|xong|tiếp|và|va|với|voi)\\s+", "");
        clean = clean.replaceAll("[\\.,;:!\\?~\\-_]+$", "").trim();
        clean = clean.replaceAll("^[\\.,;:!\\?~\\-_]+", "").trim();
        clean = clean.replaceAll("(?iU)^(?:sau đó|sau do|tiếp theo|tiep theo|tiếp đến|tiep den|kế tiếp|ke tiep|xong rồi|xong roi|xong thì|xong thi|rồi|roi|xong|tiếp|và|va|với|voi)\\s+", "");
        clean = clean.replaceAll("\\s+", " ").trim();

        if (clean.length() > 1) {
            clean = clean.substring(0, 1).toUpperCase() + clean.substring(1);
        } else if (clean.isEmpty()) {
            clean = "Công việc mới";
        }
        return clean;
    }

    private String normalizeAiCategory(String value) {
        String normalized = value != null ? value.trim().toUpperCase(Locale.ROOT) : "";
        return Set.of("WORK", "STUDY", "HEALTH", "ENTERTAINMENT").contains(normalized) ? normalized : "WORK";
    }

    private String normalizeAiPriority(String value) {
        String normalized = value != null ? value.trim().toUpperCase(Locale.ROOT) : "";
        return Set.of("LOW", "MEDIUM", "HIGH", "URGENT").contains(normalized) ? normalized : "MEDIUM";
    }

    private String limitText(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
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

            List<KhoiThoiGian> existingBlocks = timeBlockRepository.findByUserIdAndStartTimeBetween(userId, windowStart,
                    windowEnd);

            if (existingBlocks != null && !existingBlocks.isEmpty()) {
                existingBlocks.sort(Comparator.comparing(KhoiThoiGian::getStartTime));

                for (KhoiThoiGian b : existingBlocks) {
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
    private OffsetDateTime findNextAvailableFreeSlot(List<KhoiThoiGian> existingBlocks, OffsetDateTime candidateStart, int durationMinutes) {
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
                for (KhoiThoiGian b : existingBlocks) {
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

    @Override
    @Transactional
    public List<SubtaskDto> breakdownTask(Long userId, Long taskId) {
        long startTime = System.currentTimeMillis();
        CongViec task = taskRepository.findById(taskId)
                .filter(item -> Objects.equals(item.getUserId(), userId))
                .orElseThrow(() -> new com.taskmanager.exception.ResourceNotFoundException("Không tìm thấy công việc #" + taskId));

        String prompt = String.format("Công việc: '%s'. Mô tả: '%s'. Thời lượng dự kiến: %d phút.",
                task.getTitle(), task.getDescription() != null ? task.getDescription() : "",
                task.getEstimatedDurationMinutes() != null ? task.getEstimatedDurationMinutes() : 60);

        String systemInstruction = "Vai trò: Bạn là Chuyên gia Quản lý Năng suất và Phân rã Công việc SmartTask AI.\n"
                + "Nhiệm vụ: Hãy phân tích công việc của người dùng và chia nhỏ nó thành 3 đến 5 bước công việc con (subtasks) tuần tự, khả thi, rõ ràng.\n"
                + "Mỗi bước phải có: 'title' (tiêu đề hành động ngắn gọn, bắt đầu bằng động từ tiếng Việt) và 'durationMinutes' (thời lượng phút ước tính cho bước đó, từ 10 đến 45 phút).\n"
                + "Định dạng JSON trả về bắt buộc:\n"
                + "{\n"
                + "  \"subtasks\": [\n"
                + "     {\"title\": \"string\", \"durationMinutes\": number}\n"
                + "  ]\n"
                + "}";

        List<SubtaskDto> result = new ArrayList<>();
        String rawResponse = null;

        if (geminiApiClient.isConfigured()) {
            Optional<String> geminiResult = geminiApiClient.generateJsonResponse(systemInstruction, prompt);
            if (geminiResult.isPresent()) {
                rawResponse = geminiResult.get().trim();
                try {
                    String cleanJson = rawResponse;
                    if (cleanJson.startsWith("```json")) cleanJson = cleanJson.substring(7);
                    if (cleanJson.startsWith("```")) cleanJson = cleanJson.substring(3);
                    if (cleanJson.endsWith("```")) cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
                    cleanJson = cleanJson.trim();

                    JsonNode root = objectMapper.readTree(cleanJson);
                    JsonNode array = root.path("subtasks");
                    if (array.isArray() && !array.isEmpty()) {
                        int order = 0;
                        int subtaskLimit = Math.min(array.size(), MAX_AI_SUBTASKS);
                        for (int subtaskIndex = 0; subtaskIndex < subtaskLimit; subtaskIndex++) {
                            JsonNode n = array.get(subtaskIndex);
                            String stTitle = n.path("title").asString("").trim();
                            int dur = n.path("durationMinutes").asInt(15);
                            if (!stTitle.isEmpty() && stTitle.length() <= 255 && dur >= 1 && dur <= 1440) {
                                CongViecCon subtask = CongViecCon.builder()
                                        .taskId(taskId)
                                        .title(stTitle)
                                        .isCompleted(false)
                                        .durationMinutes(dur)
                                        .orderIndex(order++)
                                        .build();
                                CongViecCon saved = subtaskRepository.save(subtask);
                                result.add(SubtaskDto.builder()
                                        .id(saved.getId())
                                        .taskId(taskId)
                                        .title(saved.getTitle())
                                        .isCompleted(saved.getIsCompleted())
                                        .durationMinutes(saved.getDurationMinutes())
                                        .orderIndex(saved.getOrderIndex())
                                        .createdAt(saved.getCreatedAt())
                                        .build());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Lỗi phân tích JSON subtasks từ Gemini ({}).", e.getClass().getSimpleName());
                }
            }
        }

        // Heuristic fallback if Gemini is offline or didn't return valid subtasks
        if (result.isEmpty()) {
            log.info("Sử dụng Heuristic Subtask Breakdown cho task #{}", task.getId());
            List<String> defaultTitles;
            String lower = task.getTitle().toLowerCase();
            if (lower.contains("ôn") || lower.contains("thi") || lower.contains("học") || lower.contains("bài")) {
                defaultTitles = List.of(
                        "Đọc lại lý thuyết & tổng hợp đề cương trọng tâm",
                        "Làm bài tập ví dụ và các dạng câu hỏi mẫu",
                        "Tự bấm giờ giải đề thi thử",
                        "Ghi chú lại các lỗi sai cần khắc phục"
                );
            } else if (lower.contains("đồ án") || lower.contains("báo cáo") || lower.contains("nghiên cứu")) {
                defaultTitles = List.of(
                        "Xác định mục tiêu & lập dàn ý chi tiết",
                        "Thu thập tài liệu và số liệu tham khảo",
                        "Thực hiện nội dung cốt lõi và lập trình/viết bản thảo",
                        "Kiểm tra, định dạng lại văn bản và tổng duyệt"
                );
            } else {
                defaultTitles = List.of(
                        "Chuẩn bị tài liệu & công cụ cần thiết",
                        "Tiến hành thực hiện các bước chính",
                        "Kiểm tra chất lượng và hoàn thiện kết quả"
                );
            }

            int stepDur = Math.max((task.getEstimatedDurationMinutes() != null ? task.getEstimatedDurationMinutes() : 60) / defaultTitles.size(), 15);
            int order = 0;
            for (String title : defaultTitles) {
                CongViecCon subtask = CongViecCon.builder()
                        .taskId(taskId)
                        .title(title)
                        .isCompleted(false)
                        .durationMinutes(stepDur)
                        .orderIndex(order++)
                        .build();
                CongViecCon saved = subtaskRepository.save(subtask);
                result.add(SubtaskDto.builder()
                        .id(saved.getId())
                        .taskId(taskId)
                        .title(saved.getTitle())
                        .isCompleted(saved.getIsCompleted())
                        .durationMinutes(saved.getDurationMinutes())
                        .orderIndex(saved.getOrderIndex())
                        .createdAt(saved.getCreatedAt())
                        .build());
            }
        }

        // Log to nhatkyai
        try {
            NhatKyAi aiLog = NhatKyAi.builder()
                    .userId(userId)
                    .actionType("TASK_BREAKDOWN")
                    .promptInput("TASK_BREAKDOWN #" + taskId)
                    .aiResponse("{\"status\":\"completed\"}")
                    .status("SUCCESS")
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
            aiSchedulingLogRepository.save(aiLog);
        } catch (Exception ignored) {}

        return result;
    }

    @Override
    @Transactional
    public List<SubtaskDto> breakdownTaskByPrompt(Long userId, com.taskmanager.dto.request.AiBreakdownRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu phân rã công việc không hợp lệ.");
        }
        if (request.getTaskId() != null && request.getTaskId() > 0) {
            return breakdownTask(userId, request.getTaskId());
        }

        long startTime = System.currentTimeMillis();
        String title = request.getTitle() != null ? request.getTitle().trim() : "Công việc mới";
        String description = request.getDescription() != null ? request.getDescription().trim() : "";
        if (title.length() > 255 || description.length() > 4000) {
            throw new IllegalArgumentException("Tiêu đề tối đa 255 ký tự, mô tả tối đa 4.000 ký tự.");
        }
        if (request.getDurationMinutes() != null
                && (request.getDurationMinutes() < 1 || request.getDurationMinutes() > 1440)) {
            throw new IllegalArgumentException("Thời lượng ước tính phải từ 1 đến 1.440 phút.");
        }
        int estimatedDuration = request.getDurationMinutes() != null ? request.getDurationMinutes() : 60;

        String prompt = String.format("Công việc: '%s'. Mô tả: '%s'. Thời lượng dự kiến: %d phút.",
                title, description, estimatedDuration);

        String systemInstruction = "Vai trò: Bạn là Chuyên gia Quản lý Năng suất và Phân rã Công việc SmartTask AI.\n"
                + "Nhiệm vụ: Hãy phân tích công việc của người dùng và chia nhỏ nó thành 3 đến 5 bước công việc con (subtasks) tuần tự, khả thi, rõ ràng.\n"
                + "Mỗi bước phải có: 'title' (tiêu đề hành động ngắn gọn, bắt đầu bằng động từ tiếng Việt) và 'durationMinutes' (thời lượng phút ước tính cho bước đó, từ 10 đến 45 phút).\n"
                + "Định dạng JSON trả về bắt buộc:\n"
                + "{\n"
                + "  \"subtasks\": [\n"
                + "     {\"title\": \"string\", \"durationMinutes\": number}\n"
                + "  ]\n"
                + "}";

        List<SubtaskDto> result = new ArrayList<>();
        String rawResponse = null;

        if (geminiApiClient.isConfigured()) {
            Optional<String> geminiResult = geminiApiClient.generateJsonResponse(systemInstruction, prompt);
            if (geminiResult.isPresent()) {
                rawResponse = geminiResult.get().trim();
                try {
                    String cleanJson = rawResponse;
                    if (cleanJson.startsWith("```json")) cleanJson = cleanJson.substring(7);
                    if (cleanJson.startsWith("```")) cleanJson = cleanJson.substring(3);
                    if (cleanJson.endsWith("```")) cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
                    cleanJson = cleanJson.trim();

                    JsonNode root = objectMapper.readTree(cleanJson);
                    JsonNode array = root.path("subtasks");
                    if (array.isArray() && !array.isEmpty()) {
                        int order = 0;
                        long fakeId = -System.currentTimeMillis();
                        int subtaskLimit = Math.min(array.size(), MAX_AI_SUBTASKS);
                        for (int subtaskIndex = 0; subtaskIndex < subtaskLimit; subtaskIndex++) {
                            JsonNode n = array.get(subtaskIndex);
                            String stTitle = n.path("title").asString("").trim();
                            int dur = n.path("durationMinutes").asInt(15);
                            if (!stTitle.isEmpty() && stTitle.length() <= 255 && dur >= 1 && dur <= 1440) {
                                result.add(SubtaskDto.builder()
                                        .id(fakeId--)
                                        .taskId(0L)
                                        .title(stTitle)
                                        .isCompleted(false)
                                        .durationMinutes(dur)
                                        .orderIndex(order++)
                                        .createdAt(OffsetDateTime.now())
                                        .build());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Lỗi phân tích JSON subtasks từ Gemini cho preview ({}).", e.getClass().getSimpleName());
                }
            }
        }

        // Fallback heuristic if Gemini API key is offline or limit reached
        if (result.isEmpty()) {
            List<String> defaultTitles;
            String lower = title.toLowerCase();
            if (lower.contains("ôn") || lower.contains("thi") || lower.contains("học") || lower.contains("bài")) {
                defaultTitles = List.of(
                        "Đọc lại lý thuyết & tổng hợp đề cương trọng tâm",
                        "Làm bài tập ví dụ và các dạng câu hỏi mẫu",
                        "Tự bấm giờ giải đề thi thử",
                        "Ghi chú lại các lỗi sai cần khắc phục"
                );
            } else if (lower.contains("đồ án") || lower.contains("báo cáo") || lower.contains("nghiên cứu")) {
                defaultTitles = List.of(
                        "Xác định mục tiêu & lập dàn ý chi tiết",
                        "Thu thập tài liệu và số liệu tham khảo",
                        "Thực hiện nội dung cốt lõi và hoàn thiện bản thảo",
                        "Kiểm tra, định dạng lại văn bản và tổng duyệt"
                );
            } else {
                defaultTitles = List.of(
                        limitText("Chuẩn bị tài liệu & công cụ cho: " + title, 255),
                        limitText("Thực hiện các bước trọng tâm của việc: " + title, 255),
                        "Kiểm tra chất lượng và hoàn thiện kết quả"
                );
            }

            long fakeId = -System.currentTimeMillis();
            int order = 0;
            for (String dt : defaultTitles) {
                result.add(SubtaskDto.builder()
                        .id(fakeId--)
                        .taskId(0L)
                        .title(dt)
                        .isCompleted(false)
                        .durationMinutes(20)
                        .orderIndex(order++)
                        .createdAt(OffsetDateTime.now())
                        .build());
            }
        }

        // Log to nhatkyai
        try {
            NhatKyAi aiLog = NhatKyAi.builder()
                    .userId(userId)
                    .actionType("TASK_BREAKDOWN_PREVIEW")
                    .promptInput("TASK_BREAKDOWN_PROMPT")
                    .aiResponse("{\"status\":\"completed\"}")
                    .status("SUCCESS")
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
            aiSchedulingLogRepository.save(aiLog);
        } catch (Exception ignored) {}

        return result;
    }

    @Override
    @Transactional
    public AutoScheduleDto.Response autoSchedule(Long userId, AutoScheduleDto request) {
        long startTime = System.currentTimeMillis();
        if (userId == null) {
            throw new IllegalArgumentException("Yêu cầu cần phiên đăng nhập hợp lệ.");
        }
        if (request == null) {
            request = new AutoScheduleDto();
        }
        if (request.getTaskIds() != null && request.getTaskIds().size() > 500) {
            throw new IllegalArgumentException("Chỉ có thể xếp lịch tối đa 500 công việc mỗi lần.");
        }

        // null means "all eligible tasks"; an explicitly empty list means "schedule none".
        boolean restrictToTaskIds = request.getTaskIds() != null;
        List<Long> requestedTaskIds = request.getTaskIds() == null ? List.of() : request.getTaskIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        CaiDatNguoiDung pref = userPreferenceRepository.findByUserId(userId).orElse(null);
        ZoneId scheduleZone = resolveScheduleZone(pref);
        LocalDate today = LocalDate.now(scheduleZone);
        LocalDate targetDate;
        if (request.getTargetDate() != null && !request.getTargetDate().isBlank()) {
            try {
                targetDate = LocalDate.parse(request.getTargetDate().trim());
            } catch (Exception e) {
                throw new IllegalArgumentException("Ngày cần xếp lịch không hợp lệ.");
            }
        } else {
            targetDate = today;
        }
        if (targetDate.isBefore(today)) {
            throw new IllegalArgumentException("Không thể tự động xếp lịch vào ngày đã qua.");
        }

        LocalTime workStart = (pref != null && pref.getWorkStartTime() != null) ? pref.getWorkStartTime() : LocalTime.of(8, 0);
        LocalTime workEnd = (pref != null && pref.getWorkEndTime() != null) ? pref.getWorkEndTime() : LocalTime.of(18, 0);
        int bufferMinutes = (pref != null && pref.getBufferMinutes() != null) ? pref.getBufferMinutes() : 15;
        if (!workEnd.isAfter(workStart) || bufferMinutes < 0 || bufferMinutes > 240) {
            throw new IllegalStateException("Khung giờ làm việc hoặc thời gian nghỉ giữa phiên chưa hợp lệ.");
        }

        OffsetDateTime dayStart = targetDate.atTime(workStart).atZone(scheduleZone).toOffsetDateTime();
        OffsetDateTime dayEnd = targetDate.atTime(workEnd).atZone(scheduleZone).toOffsetDateTime();
        if (!dayEnd.isAfter(dayStart)) {
            throw new IllegalStateException("Khung giờ làm việc trong ngày không hợp lệ.");
        }

        OffsetDateTime scheduleNow = OffsetDateTime.now(scheduleZone);
        OffsetDateTime nextWholeMinute = scheduleNow.truncatedTo(ChronoUnit.MINUTES);
        if (scheduleNow.getSecond() != 0 || scheduleNow.getNano() != 0) {
            nextWholeMinute = nextWholeMinute.plusMinutes(1);
        }
        if (targetDate.equals(today) && nextWholeMinute.isAfter(dayStart)) {
            dayStart = nextWholeMinute.isBefore(dayEnd) ? nextWholeMinute : dayEnd;
        }

        ScheduleSlot breakWindow = resolveBreakWindow(request, targetDate, scheduleZone, dayStart, dayEnd);
        Set<DayOfWeek> workDays = parseWorkingDays(pref != null ? pref.getWorkDays() : null);
        boolean isWorkDay = workDays.contains(targetDate.getDayOfWeek());

        // Query only time blocks intersecting this day; fetch task IDs separately to prevent
        // duplicate schedules across dates without loading the user's full calendar into memory.
        OffsetDateTime calendarDayStart = targetDate.atStartOfDay(scheduleZone).toOffsetDateTime();
        OffsetDateTime calendarDayEnd = targetDate.plusDays(1).atStartOfDay(scheduleZone).toOffsetDateTime();
        List<KhoiThoiGian> existingBlocks = timeBlockRepository
                .findByUserIdAndStartTimeBeforeAndEndTimeAfter(userId,
                        calendarDayEnd.plusMinutes(bufferMinutes), calendarDayStart.minusMinutes(bufferMinutes))
                .stream()
                .filter(this::isActiveTimeBlock)
                .sorted(Comparator.comparing(KhoiThoiGian::getStartTime))
                .toList();
        Set<Long> alreadyScheduledTaskIds = new HashSet<>(timeBlockRepository.findScheduledTaskIdsByUserId(userId));

        // Get candidate tasks
        List<CongViec> candidateTasks;
        if (restrictToTaskIds && requestedTaskIds.isEmpty()) {
            candidateTasks = List.of();
        } else if (restrictToTaskIds) {
            candidateTasks = taskRepository.findByIdInAndUserId(requestedTaskIds, userId);
        } else if (request.getWorkspaceId() != null) {
            candidateTasks = taskRepository.findByUserIdAndWorkspaceId(userId, request.getWorkspaceId());
        } else {
            candidateTasks = taskRepository.findByUserId(userId);
        }
        if (restrictToTaskIds && request.getWorkspaceId() != null) {
            Long requestedWorkspaceId = request.getWorkspaceId();
            candidateTasks = candidateTasks.stream()
                    .filter(task -> Objects.equals(task.getWorkspaceId(), requestedWorkspaceId))
                    .toList();
        }

        Map<Long, String> categoryTypesById = new HashMap<>();
        categoryRepository.findByUserId(userId).forEach(category ->
                categoryTypesById.put(category.getId(), normalizeCategoryType(category.getName())));

        // Keep locked tasks and fixed appointments out of the flexible scheduling pass.
        candidateTasks = candidateTasks.stream()
                .filter(task -> !alreadyScheduledTaskIds.contains(task.getId()))
                .filter(task -> !Boolean.TRUE.equals(task.getIsLocked()))
                .filter(t -> "TODO".equalsIgnoreCase(t.getStatus())
                        || "IN_PROGRESS".equalsIgnoreCase(t.getStatus())
                        || "OVERDUE".equalsIgnoreCase(t.getStatus()))
                .filter(task -> isTaskRelevantOnDate(task, targetDate, scheduleZone))
                .sorted(buildTaskPriorityComparator(targetDate, scheduleZone))
                .toList();

        List<AutoScheduleDto.ScheduledTaskProposal> proposals = new ArrayList<>();
        Set<Long> fixedTaskIds = new HashSet<>();
        List<ScheduleSlot> freeSlots = isWorkDay
                ? buildFreeSlots(dayStart, dayEnd, breakWindow, existingBlocks, bufferMinutes)
                : new ArrayList<>();

        // Fixed tasks retain their exact time. They are never moved by the flexible scheduler.
        List<CongViec> fixedTasks = candidateTasks.stream().filter(this::isFixedTask).toList();
        List<ScheduleSlot> acceptedFixedSlots = new ArrayList<>();
        for (CongViec task : fixedTasks) {
            OffsetDateTime fixedStart = task.getFixedStartTime();
            OffsetDateTime fixedEnd = task.getFixedEndTime();
            if (fixedStart == null || fixedEnd == null) continue;
            long fixedDurationSeconds = Duration.between(fixedStart, fixedEnd).getSeconds();
            if (!fixedEnd.isAfter(fixedStart)
                    || fixedDurationSeconds < 60
                    || fixedDurationSeconds > 86400
                    || fixedDurationSeconds % 60 != 0
                    || !fixedStart.atZoneSameInstant(scheduleZone).toLocalDate().equals(targetDate)) {
                continue;
            }
            if ((targetDate.equals(today) && fixedStart.isBefore(nextWholeMinute))
                    || hasOverlapBlocks(existingBlocks,
                            fixedStart.minusMinutes(bufferMinutes), fixedEnd.plusMinutes(bufferMinutes))
                    || hasOverlapSlots(acceptedFixedSlots,
                            fixedStart.minusMinutes(bufferMinutes), fixedEnd.plusMinutes(bufferMinutes))) {
                continue;
            }

            fixedTaskIds.add(task.getId());
            acceptedFixedSlots.add(new ScheduleSlot(fixedStart, fixedEnd));
            reserveInterval(freeSlots, fixedStart, fixedEnd, bufferMinutes);
            proposals.add(buildScheduleProposal(task, fixedStart, fixedEnd, categoryTypesById,
                    "Giữ nguyên khung giờ cố định đã chọn."));
        }

        ScheduleSlot peakWindow = isWorkDay
                ? resolvePeakWindow(pref, targetDate, scheduleZone, workStart, workEnd)
                : null;
        List<CongViec> flexibleTasks = candidateTasks.stream()
                .filter(task -> !isFixedTask(task))
                .limit(500)
                .toList();

        for (CongViec task : flexibleTasks) {
            Integer configuredDuration = task.getEstimatedDurationMinutes();
            int duration = configuredDuration == null || configuredDuration == 0 ? 45 : configuredDuration;
            if (duration < 1 || duration > 1440 || freeSlots.isEmpty()) {
                continue;
            }

            SlotChoice choice = findBestSlot(freeSlots, duration, isFocusTask(task), peakWindow);
            if (choice == null) continue;

            OffsetDateTime slotStart = choice.start();
            OffsetDateTime slotEnd = slotStart.plusMinutes(duration);
            String rationale = generateRationale(task, slotStart, scheduleZone);
            if (isFocusTask(task) && peakWindow != null && !slotStart.isBefore(peakWindow.start())
                    && slotEnd.compareTo(peakWindow.end()) <= 0) {
                rationale += " Ưu tiên khung giờ hiệu suất cao đã cài đặt.";
            }
            proposals.add(buildScheduleProposal(task, slotStart, slotEnd, categoryTypesById, rationale));
            reserveSlot(freeSlots, choice.slotIndex(), slotStart, slotEnd, bufferMinutes);
        }

        proposals.sort(Comparator.comparing(AutoScheduleDto.ScheduledTaskProposal::getStartTime));

        if (Boolean.TRUE.equals(request.getApplyImmediately())) {
            Map<Long, CongViec> tasksById = candidateTasks.stream().collect(Collectors.toMap(
                    CongViec::getId, task -> task, (first, ignored) -> first));
            for (AutoScheduleDto.ScheduledTaskProposal proposal : proposals) {
                CongViec task = tasksById.get(proposal.getTaskId());
                if (task == null) continue;
                timeBlockRepository.save(KhoiThoiGian.builder()
                        .userId(userId)
                        .taskId(task.getId())
                        .title(task.getTitle())
                        .startTime(proposal.getStartTime())
                        .endTime(proposal.getEndTime())
                        .isAutoScheduled(!fixedTaskIds.contains(task.getId()))
                        .colorCode("#6C5CE7")
                        .note(fixedTaskIds.contains(task.getId()) ? "Giữ nguyên khung giờ cố định." :
                                "Tự động xếp lịch: " + proposal.getSlotRationale())
                        .isLocked(Boolean.TRUE.equals(task.getIsLocked()))
                        .build());
            }
        }

        int remainingUnscheduled = Math.max(0, candidateTasks.size() - proposals.size());
        String summary = buildScheduleSummary(targetDate, isWorkDay, proposals.size(),
                remainingUnscheduled, bufferMinutes, pref);

        try {
            NhatKyAi aiLog = NhatKyAi.builder()
                    .userId(userId)
                    .actionType("AUTO_SCHEDULE")
                    .promptInput("AUTO_SCHEDULE " + targetDate + " tasks=" + proposals.size())
                    .aiResponse("{\"scheduledCount\":" + proposals.size() + "}")
                    .status("SUCCESS")
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
            aiSchedulingLogRepository.save(aiLog);
        } catch (Exception ignored) {}

        return AutoScheduleDto.Response.builder()
                .targetDate(targetDate.toString())
                .scheduledCount(proposals.size())
                .remainingUnscheduled(remainingUnscheduled)
                .proposals(proposals)
                .aiStrategySummary(summary)
                .applied(Boolean.TRUE.equals(request.getApplyImmediately()) && !proposals.isEmpty())
                .build();
    }

    private ZoneId resolveScheduleZone(CaiDatNguoiDung preference) {
        if (preference != null && preference.getTimeZone() != null && !preference.getTimeZone().isBlank()) {
            try {
                return ZoneId.of(preference.getTimeZone().trim());
            } catch (DateTimeException ignored) {
                log.warn("Múi giờ trong thiết lập không hợp lệ; dùng Asia/Ho_Chi_Minh cho lịch tự động.");
            }
        }
        return VIETNAM_ZONE;
    }

    private Set<DayOfWeek> parseWorkingDays(String configuredDays) {
        if (configuredDays == null) {
            return EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
        }
        if (configuredDays.isBlank()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }

        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        for (String rawDay : configuredDays.split("[,;\\s]+")) {
            String token = rawDay.trim().toUpperCase(Locale.ROOT);
            if (token.length() > 3) token = token.substring(0, 3);
            switch (token) {
                case "MON" -> days.add(DayOfWeek.MONDAY);
                case "TUE" -> days.add(DayOfWeek.TUESDAY);
                case "WED" -> days.add(DayOfWeek.WEDNESDAY);
                case "THU" -> days.add(DayOfWeek.THURSDAY);
                case "FRI" -> days.add(DayOfWeek.FRIDAY);
                case "SAT" -> days.add(DayOfWeek.SATURDAY);
                case "SUN" -> days.add(DayOfWeek.SUNDAY);
                default -> { }
            }
        }
        return days.isEmpty() ? EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY) : days;
    }

    private ScheduleSlot resolveBreakWindow(
            AutoScheduleDto request,
            LocalDate targetDate,
            ZoneId zone,
            OffsetDateTime dayStart,
            OffsetDateTime dayEnd) {
        String startText = request.getBreakStartTime();
        String endText = request.getBreakEndTime();
        if ((startText == null || startText.isBlank()) && (endText == null || endText.isBlank())) {
            startText = "12:00";
            endText = "13:00";
        } else if (startText == null || startText.isBlank() || endText == null || endText.isBlank()) {
            throw new IllegalArgumentException("Cần chọn đầy đủ giờ bắt đầu và kết thúc giờ nghỉ.");
        }

        LocalTime breakStart;
        LocalTime breakEnd;
        try {
            breakStart = LocalTime.parse(startText.trim());
            breakEnd = LocalTime.parse(endText.trim());
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Giờ nghỉ không đúng định dạng HH:mm.");
        }
        if (!breakEnd.isAfter(breakStart)) {
            throw new IllegalArgumentException("Giờ kết thúc nghỉ phải sau giờ bắt đầu nghỉ.");
        }

        OffsetDateTime requestedStart = targetDate.atTime(breakStart).atZone(zone).toOffsetDateTime();
        OffsetDateTime requestedEnd = targetDate.atTime(breakEnd).atZone(zone).toOffsetDateTime();
        OffsetDateTime clippedStart = requestedStart.isAfter(dayStart) ? requestedStart : dayStart;
        OffsetDateTime clippedEnd = requestedEnd.isBefore(dayEnd) ? requestedEnd : dayEnd;
        return clippedEnd.isAfter(clippedStart) ? new ScheduleSlot(clippedStart, clippedEnd) : null;
    }

    private boolean isActiveTimeBlock(KhoiThoiGian block) {
        if (block.getStartTime() == null || block.getEndTime() == null
                || !block.getEndTime().isAfter(block.getStartTime())) {
            return false;
        }
        String status = block.getStatus() != null ? block.getStatus().trim().toUpperCase(Locale.ROOT) : "";
        return !Set.of("CANCELLED", "CANCELED", "DELETED").contains(status);
    }

    private boolean isTaskRelevantOnDate(CongViec task, LocalDate date, ZoneId zone) {
        if (!isFixedTask(task)) return true;
        OffsetDateTime fixedAnchor = task.getFixedStartTime() != null ? task.getFixedStartTime() : task.getFixedEndTime();
        return fixedAnchor == null || fixedAnchor.atZoneSameInstant(zone).toLocalDate().equals(date);
    }

    private boolean isFixedTask(CongViec task) {
        return Boolean.TRUE.equals(task.getIsFixedTime())
                || task.getFixedStartTime() != null
                || task.getFixedEndTime() != null;
    }

    private Comparator<CongViec> buildTaskPriorityComparator(LocalDate targetDate, ZoneId zone) {
        return Comparator.comparingInt((CongViec task) -> deadlineRank(task, targetDate, zone))
                .thenComparingInt(task -> getQuadrantRank(task.getEisenhowerQuadrant(), task.getPriority()))
                .thenComparingInt(task -> priorityRank(task.getPriority()))
                .thenComparing(task -> task.getDueDate() != null ? task.getDueDate().toInstant() : Instant.MAX)
                .thenComparing(Comparator.comparingInt(this::normalizedTaskDuration).reversed())
                .thenComparing(CongViec::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(CongViec::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private int deadlineRank(CongViec task, LocalDate targetDate, ZoneId zone) {
        if ("OVERDUE".equalsIgnoreCase(task.getStatus())) return 0;
        if (task.getDueDate() == null) return 6;
        long daysUntilDeadline = ChronoUnit.DAYS.between(
                targetDate,
                task.getDueDate().atZoneSameInstant(zone).toLocalDate());
        if (daysUntilDeadline < 0) return 0;
        if (daysUntilDeadline == 0) return 1;
        if (daysUntilDeadline == 1) return 2;
        if (daysUntilDeadline <= 3) return 3;
        if (daysUntilDeadline <= 7) return 4;
        return 5;
    }

    private int priorityRank(String priority) {
        if ("URGENT".equalsIgnoreCase(priority)) return 0;
        if ("HIGH".equalsIgnoreCase(priority)) return 1;
        if ("LOW".equalsIgnoreCase(priority)) return 3;
        return 2;
    }

    private int normalizedTaskDuration(CongViec task) {
        Integer duration = task.getEstimatedDurationMinutes();
        return duration != null && duration > 0 && duration <= 1440 ? duration : 45;
    }

    private boolean isFocusTask(CongViec task) {
        return getQuadrantRank(task.getEisenhowerQuadrant(), task.getPriority()) <= 2
                || "URGENT".equalsIgnoreCase(task.getPriority())
                || "HIGH".equalsIgnoreCase(task.getPriority());
    }

    private ScheduleSlot resolvePeakWindow(
            CaiDatNguoiDung preference,
            LocalDate date,
            ZoneId zone,
            LocalTime workStart,
            LocalTime workEnd) {
        String peak = preference != null && preference.getPeakProductivityTime() != null
                ? preference.getPeakProductivityTime().trim().toUpperCase(Locale.ROOT)
                : "MORNING";
        LocalTime preferredStart;
        LocalTime preferredEnd;
        switch (peak) {
            case "AFTERNOON" -> {
                preferredStart = LocalTime.of(13, 0);
                preferredEnd = LocalTime.of(17, 0);
            }
            case "EVENING" -> {
                preferredStart = LocalTime.of(17, 0);
                preferredEnd = workEnd;
            }
            default -> {
                preferredStart = workStart;
                preferredEnd = LocalTime.NOON;
            }
        }
        LocalTime clippedStart = preferredStart.isAfter(workStart) ? preferredStart : workStart;
        LocalTime clippedEnd = preferredEnd.isBefore(workEnd) ? preferredEnd : workEnd;
        if (!clippedEnd.isAfter(clippedStart)) return null;
        return new ScheduleSlot(date.atTime(clippedStart).atZone(zone).toOffsetDateTime(),
                date.atTime(clippedEnd).atZone(zone).toOffsetDateTime());
    }

    private List<ScheduleSlot> buildFreeSlots(
            OffsetDateTime dayStart,
            OffsetDateTime dayEnd,
            ScheduleSlot breakWindow,
            List<KhoiThoiGian> existingBlocks,
            int bufferMinutes) {
        List<ScheduleSlot> occupied = new ArrayList<>();
        for (KhoiThoiGian block : existingBlocks) {
            OffsetDateTime paddedStart = block.getStartTime().minusMinutes(bufferMinutes);
            OffsetDateTime blockStart = paddedStart.isAfter(dayStart) ? paddedStart : dayStart;
            OffsetDateTime bufferedEnd = block.getEndTime().plusMinutes(bufferMinutes);
            OffsetDateTime blockEnd = bufferedEnd.isBefore(dayEnd) ? bufferedEnd : dayEnd;
            if (blockEnd.isAfter(blockStart)) occupied.add(new ScheduleSlot(blockStart, blockEnd));
        }
        if (breakWindow != null) occupied.add(breakWindow);
        occupied.sort(Comparator.comparing(ScheduleSlot::start));

        List<ScheduleSlot> merged = new ArrayList<>();
        for (ScheduleSlot slot : occupied) {
            if (merged.isEmpty() || slot.start().isAfter(merged.get(merged.size() - 1).end())) {
                merged.add(slot);
            } else {
                ScheduleSlot previous = merged.remove(merged.size() - 1);
                OffsetDateTime end = slot.end().isAfter(previous.end()) ? slot.end() : previous.end();
                merged.add(new ScheduleSlot(previous.start(), end));
            }
        }

        List<ScheduleSlot> free = new ArrayList<>();
        OffsetDateTime cursor = dayStart;
        for (ScheduleSlot slot : merged) {
            if (slot.start().isAfter(cursor)) free.add(new ScheduleSlot(cursor, slot.start()));
            if (slot.end().isAfter(cursor)) cursor = slot.end();
        }
        if (dayEnd.isAfter(cursor)) free.add(new ScheduleSlot(cursor, dayEnd));
        return free;
    }

    private SlotChoice findBestSlot(List<ScheduleSlot> freeSlots, int duration, boolean focus, ScheduleSlot peakWindow) {
        if (focus && peakWindow != null) {
            for (int i = 0; i < freeSlots.size(); i++) {
                ScheduleSlot slot = freeSlots.get(i);
                OffsetDateTime candidateStart = slot.start().isAfter(peakWindow.start()) ? slot.start() : peakWindow.start();
                OffsetDateTime candidateEnd = candidateStart.plusMinutes(duration);
                OffsetDateTime latestEnd = slot.end().isBefore(peakWindow.end()) ? slot.end() : peakWindow.end();
                if (!candidateStart.isBefore(latestEnd)) continue;
                if (!candidateEnd.isAfter(latestEnd)) return new SlotChoice(i, candidateStart);
            }
        }
        for (int i = 0; i < freeSlots.size(); i++) {
            ScheduleSlot slot = freeSlots.get(i);
            if (!slot.start().plusMinutes(duration).isAfter(slot.end())) return new SlotChoice(i, slot.start());
        }
        return null;
    }

    private void reserveSlot(
            List<ScheduleSlot> freeSlots,
            int slotIndex,
            OffsetDateTime start,
            OffsetDateTime end,
            int bufferMinutes) {
        ScheduleSlot original = freeSlots.remove(slotIndex);
        if (start.isAfter(original.start())) freeSlots.add(new ScheduleSlot(original.start(), start));
        OffsetDateTime nextAvailable = end.plusMinutes(bufferMinutes);
        if (original.end().isAfter(nextAvailable)) freeSlots.add(new ScheduleSlot(nextAvailable, original.end()));
        freeSlots.sort(Comparator.comparing(ScheduleSlot::start));
    }

    private void reserveInterval(List<ScheduleSlot> freeSlots, OffsetDateTime start, OffsetDateTime end, int bufferMinutes) {
        if (!end.isAfter(start)) return;
        OffsetDateTime reservedStart = start.minusMinutes(bufferMinutes);
        OffsetDateTime reservedEnd = end.plusMinutes(bufferMinutes);
        List<ScheduleSlot> remaining = new ArrayList<>();
        for (ScheduleSlot slot : freeSlots) {
            if (!overlaps(slot.start(), slot.end(), reservedStart, reservedEnd)) {
                remaining.add(slot);
                continue;
            }
            if (reservedStart.isAfter(slot.start())) remaining.add(new ScheduleSlot(slot.start(), reservedStart));
            if (slot.end().isAfter(reservedEnd)) remaining.add(new ScheduleSlot(reservedEnd, slot.end()));
        }
        remaining.sort(Comparator.comparing(ScheduleSlot::start));
        freeSlots.clear();
        freeSlots.addAll(remaining);
    }

    private boolean hasOverlapBlocks(List<KhoiThoiGian> blocks, OffsetDateTime start, OffsetDateTime end) {
        return blocks.stream().anyMatch(block -> overlaps(block.getStartTime(), block.getEndTime(), start, end));
    }

    private boolean hasOverlapSlots(List<ScheduleSlot> slots, OffsetDateTime start, OffsetDateTime end) {
        return slots.stream().anyMatch(slot -> overlaps(slot.start(), slot.end(), start, end));
    }

    private boolean overlaps(OffsetDateTime firstStart, OffsetDateTime firstEnd,
                             OffsetDateTime secondStart, OffsetDateTime secondEnd) {
        return firstStart.isBefore(secondEnd) && firstEnd.isAfter(secondStart);
    }

    private AutoScheduleDto.ScheduledTaskProposal buildScheduleProposal(
            CongViec task,
            OffsetDateTime start,
            OffsetDateTime end,
            Map<Long, String> categoryTypesById,
            String rationale) {
        return AutoScheduleDto.ScheduledTaskProposal.builder()
                .taskId(task.getId())
                .title(task.getTitle())
                .categoryType(categoryTypesById.getOrDefault(task.getCategoryId(), "WORK"))
                .priority(task.getPriority())
                .eisenhowerQuadrant(task.getEisenhowerQuadrant())
                .startTime(start)
                .endTime(end)
                .durationMinutes((int) Duration.between(start, end).toMinutes())
                .slotRationale(rationale)
                .build();
    }

    private String normalizeCategoryType(String categoryName) {
        String normalized = categoryName != null ? categoryName.trim().toLowerCase(Locale.ROOT) : "";
        if (normalized.contains("học") || normalized.contains("study")) return "STUDY";
        if (normalized.contains("sức khỏe") || normalized.contains("health")) return "HEALTH";
        if (normalized.contains("giải trí") || normalized.contains("entertainment")) return "ENTERTAINMENT";
        return "WORK";
    }

    private String buildScheduleSummary(
            LocalDate date,
            boolean workDay,
            int scheduledCount,
            int remainingCount,
            int bufferMinutes,
            CaiDatNguoiDung preference) {
        String dateText = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        if (!workDay) {
            return String.format("Ngày %s không nằm trong ngày làm việc đã cài đặt. Lịch cố định được giữ nguyên; không xếp thêm việc linh hoạt.", dateText);
        }
        String peak = preference != null && preference.getPeakProductivityTime() != null
                ? preference.getPeakProductivityTime().trim().toLowerCase(Locale.ROOT)
                : "buổi sáng";
        return String.format("Đã xếp %d việc cho ngày %s theo hạn chót, mức ưu tiên và ma trận Eisenhower; việc cần tập trung được ưu tiên vào khung hiệu suất cao (%s). Đã chừa %d phút giữa các phiên; còn %d việc chưa có khung phù hợp.",
                scheduledCount, dateText, peak, bufferMinutes, remainingCount);
    }

    private int getQuadrantRank(String quadrant, String priority) {
        if ("DO_FIRST".equalsIgnoreCase(quadrant) || "URGENT".equalsIgnoreCase(priority)) return 1;
        if ("SCHEDULE".equalsIgnoreCase(quadrant) || "HIGH".equalsIgnoreCase(priority)) return 2;
        if ("DELEGATE".equalsIgnoreCase(quadrant)) return 3;
        if ("DONT_DO".equalsIgnoreCase(quadrant) || "LOW".equalsIgnoreCase(priority)) return 4;
        return 2;
    }

    private String generateRationale(CongViec task, OffsetDateTime slot, ZoneId zone) {
        String quad = task.getEisenhowerQuadrant() != null ? task.getEisenhowerQuadrant().toUpperCase() : "SCHEDULE";
        int hour = slot.getHour();
        String timeOfDay = (hour < 12) ? "buổi sáng" : (hour < 17) ? "buổi chiều" : "buổi tối";
        String rationale = switch (quad) {
            case "DO_FIRST" -> "Ưu tiên đầu ngày cho việc khẩn cấp và quan trọng (Q1).";
            case "SCHEDULE" -> "Dành khung " + timeOfDay + " cho việc quan trọng cần lên lịch (Q2).";
            case "DELEGATE" -> "Xếp việc Q3 sau các nhiệm vụ ưu tiên cao.";
            case "DONT_DO" -> "Xếp việc Q4 sau khi đã phân bổ các nhiệm vụ ưu tiên hơn.";
            default -> "Xếp theo mức ưu tiên và khung giờ còn trống.";
        };
        if ("URGENT".equalsIgnoreCase(task.getPriority()) || "HIGH".equalsIgnoreCase(task.getPriority())) {
            rationale += " Mức ưu tiên " + task.getPriority().toLowerCase(Locale.ROOT) + ".";
        }
        if (task.getDueDate() != null) {
            String deadline = task.getDueDate().atZoneSameInstant(zone)
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            rationale += " Hạn chót: " + deadline + ".";
        }
        return rationale;
    }

}

