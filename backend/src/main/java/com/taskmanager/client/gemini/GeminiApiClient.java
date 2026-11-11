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

    @Value("${gemini.api.model:gemini-1.5-flash}")
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
     * @param userPrompt         Nội dung câu lệnh của người dùng kèm ngữ cảnh
     * @return Chuỗi JSON trả về từ Gemini (nếu thành công)
     */
    public Optional<String> generateJsonResponse(String systemInstruction, String userPrompt) {
        if (!isConfigured()) {
            log.warn("Gemini API Key chưa được thiết lập hoặc còn ở giá trị mặc định. Bỏ qua gọi API.");
            return Optional.empty();
        }

        List<String> candidateModels = new ArrayList<>();
        if (model != null && !model.isBlank() && !model.contains("3.5")) {
            candidateModels.add(model.trim());
        }
        if (!candidateModels.contains("gemini-1.5-flash")) {
            candidateModels.add("gemini-1.5-flash");
        }
        if (!candidateModels.contains("gemini-2.0-flash")) {
            candidateModels.add("gemini-2.0-flash");
        }

        Map<String, Object> requestBody = new HashMap<>();

        // 1. System instruction
        if (systemInstruction != null && !systemInstruction.isBlank()) {
            requestBody.put("system_instruction", Map.of(
                    "parts", List.of(Map.of("text", systemInstruction))
            ));
        }

        // 2. User content
        requestBody.put("contents", List.of(
                Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", userPrompt))
                )
        ));

        // 3. Generation config with JSON mode
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("response_mime_type", "application/json");
        generationConfig.put("temperature", 0.2);
        requestBody.put("generationConfig", generationConfig);

        for (String candidateModel : candidateModels) {
            try {
                String endpoint = String.format("%s/%s:generateContent", apiUrl, candidateModel);
                log.info("Đang gọi Google Gemini Flash (model: {})...", candidateModel);

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
                            String text = part.path("text").asString();
                            if (text != null && !text.isBlank() && text.contains("{")) {
                                return Optional.of(text);
                            }
                        }
                        String generatedJsonText = parts.get(0).path("text").asString();
                        if (generatedJsonText != null && !generatedJsonText.isBlank()) {
                            return Optional.of(generatedJsonText);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Lỗi khi gọi model {} ({}). Thử model tiếp theo nếu có...", candidateModel, e.getMessage());
            }
        }

        log.warn("Tất cả các model Gemini Flash đều không phản hồi.");
        return Optional.empty();
    }
}
