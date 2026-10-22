package com.taskmanager.client.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class GeminiApiClient {

    @Value("${gemini.api.key:YOUR_GEMINI_API_KEY_HERE}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-1.5-flash}")
    private String model;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String apiUrl;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiApiClient(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
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

        try {
            String endpoint = String.format("%s/%s:generateContent?key=%s", apiUrl, model, apiKey.trim());

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

            log.info("Đang gọi Gemini 1.5 Flash API (model: {})...", model);

            String responseBody = webClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(12))
                    .block();

            if (responseBody == null || responseBody.isBlank()) {
                log.warn("Gemini API trả về chuỗi rỗng.");
                return Optional.empty();
            }

            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode candidates = rootNode.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    for (JsonNode part : parts) {
                        String text = part.path("text").asText();
                        if (text != null && !text.isBlank() && text.contains("{")) {
                            return Optional.of(text);
                        }
                    }
                    String generatedJsonText = parts.get(0).path("text").asText();
                    return Optional.ofNullable(generatedJsonText);
                }
            }

            log.warn("Không tìm thấy candidate content hợp lệ từ Gemini response: {}", responseBody);
            return Optional.empty();

        } catch (Exception e) {
            log.error("Lỗi khi gọi Google Gemini API: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
