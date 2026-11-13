package com.taskmanager.client.gemini;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class GeminiApiClient {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-3-flash-preview}")
    private String model;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String apiUrl;

    private final WebClient webClient;
    private final JsonMapper objectMapper;

    public GeminiApiClient(WebClient.Builder webClientBuilder, JsonMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * Kiểm tra xem API Key đã được cấu hình hợp lệ chưa
     */
    public boolean isConfigured() {
        return apiKey != null
                && !apiKey.isBlank()
                && !"YOUR_GEMINI_API_KEY_HERE".equalsIgnoreCase(apiKey.trim())
                && apiKey.length() > 10;
    }

    /**
     * Gửi yêu cầu sinh nội dung dạng JSON tới Gemini API
     *
     * @param systemInstruction Hướng dẫn hệ thống (System Prompt)
     * @param userPrompt        Nội dung câu lệnh của người dùng kèm ngữ cảnh
     * @return Chuỗi JSON trả về từ Gemini (nếu thành công)
     */
    public Optional<String> generateJsonResponse(String systemInstruction, String userPrompt) {
        if (!isConfigured()) {
            log.warn("Gemini API Key chưa được thiết lập hoặc còn ở giá trị mặc định. Bỏ qua gọi API.");
            return Optional.empty();
        }

        List<String> candidateModels = new ArrayList<>();
        if (model != null && !model.isBlank()) {
            candidateModels.add(model.trim());
        }
        // Thêm các model đang hoạt động thực tế trên Google AI Studio v1beta
        if (!candidateModels.contains("gemini-3-flash-preview")) {
            candidateModels.add("gemini-3-flash-preview");
        }
        if (!candidateModels.contains("gemma-4-26b-a4b-it")) {
            candidateModels.add("gemma-4-26b-a4b-it");
        }
        if (!candidateModels.contains("gemma-4-31b-it")) {
            candidateModels.add("gemma-4-31b-it");
        }
        if (!candidateModels.contains("gemini-3.5-flash-lite")) {
            candidateModels.add("gemini-3.5-flash-lite");
        }
        if (!candidateModels.contains("gemini-3.1-flash-lite")) {
            candidateModels.add("gemini-3.1-flash-lite");
        }
        if (!candidateModels.contains("gemini-3.8-flash")) {
            candidateModels.add("gemini-3.8-flash");
        }
        if (!candidateModels.contains("gemini-3.5-flash")) {
            candidateModels.add("gemini-3.5-flash");
        }
        if (!candidateModels.contains("gemini-flash-latest")) {
            candidateModels.add("gemini-flash-latest");
        }

        Map<String, Object> requestBody = new HashMap<>();

        // 1. System instruction
        if (systemInstruction != null && !systemInstruction.isBlank()) {
            requestBody.put("system_instruction", Map.of(
                    "parts", List.of(Map.of("text", systemInstruction))));
        }

        // 2. User content
        requestBody.put("contents", List.of(
                Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", userPrompt)))));

        // 3. Generation config with JSON mode
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("response_mime_type", "application/json");
        generationConfig.put("temperature", 0.2);
        requestBody.put("generationConfig", generationConfig);

        for (String candidateModel : candidateModels) {
            try {
                String endpoint = String.format("%s/%s:generateContent", apiUrl, candidateModel);
                log.info("Đang gửi yêu cầu tới Google Gemini API (model: {})...", candidateModel);

                String responseBody = webClient.post()
                        .uri(endpoint)
                        .header("x-goog-api-key", apiKey.trim())
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofSeconds(15))
                        .block();

                if (responseBody == null || responseBody.isBlank()) {
                    log.warn("Gemini API (model: {}) trả về chuỗi rỗng.", candidateModel);
                    continue;
                }

                JsonNode rootNode = objectMapper.readTree(responseBody);
                JsonNode candidates = rootNode.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode parts = candidates.get(0).path("content").path("parts");
                    if (parts.isArray() && !parts.isEmpty()) {
                        for (JsonNode part : parts) {
                            // Bỏ qua thinking/thought text nếu có
                            if (part.path("thought").asBoolean(false)) {
                                continue;
                            }
                            String text = part.path("text").asString();
                            if (text != null && !text.isBlank() && (text.contains("{") || text.contains("["))) {
                                log.info("Google Gemini (model: {}) phản hồi JSON thành công!", candidateModel);
                                return Optional.of(text);
                            }
                        }
                        String generatedJsonText = parts.get(0).path("text").asString();
                        if (generatedJsonText != null && !generatedJsonText.isBlank()) {
                            log.info("Google Gemini (model: {}) phản hồi thành công!", candidateModel);
                            return Optional.of(generatedJsonText);
                        }
                    }
                }
            } catch (Exception e) {
                String errMsg = e.getMessage() != null ? e.getMessage() : "";
                if (errMsg.contains("429") || errMsg.contains("RESOURCE_EXHAUSTED")) {
                    log.warn("⚠️ Model {} bị chạm giới hạn quota Google Free Tier (429 RESOURCE_EXHAUSTED). Thử model dự phòng...", candidateModel);
                } else if (errMsg.contains("404")) {
                    log.warn("Model {} không tồn tại trên endpoint v1beta (404 Not Found). Bỏ qua...", candidateModel);
                } else {
                    log.warn("Lỗi khi gọi model {} ({}). Thử model tiếp theo nếu có...", candidateModel, errMsg);
                }
            }
        }

        log.warn("Tất cả các model Google Gemini đều không phản hồi thành công.");
        return Optional.empty();
    }
}
