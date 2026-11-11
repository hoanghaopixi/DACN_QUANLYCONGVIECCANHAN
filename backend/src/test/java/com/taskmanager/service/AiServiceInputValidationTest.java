package com.taskmanager.service;

import com.taskmanager.client.gemini.GeminiApiClient;
import com.taskmanager.dto.request.AiBreakdownRequest;
import com.taskmanager.repository.AiSchedulingLogRepository;
import com.taskmanager.repository.CategoryRepository;
import com.taskmanager.repository.SubtaskRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.TimeBlockRepository;
import com.taskmanager.repository.UserPreferenceRepository;
import com.taskmanager.service.impl.AiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AiServiceInputValidationTest {

    private AiServiceImpl aiService;
    private GeminiApiClient geminiApiClient;
    private TimeBlockRepository timeBlockRepository;
    private TaskRepository taskRepository;
    private SubtaskRepository subtaskRepository;
    private UserPreferenceRepository userPreferenceRepository;
    private AiSchedulingLogRepository aiSchedulingLogRepository;

    @BeforeEach
    void setUp() {
        geminiApiClient = mock(GeminiApiClient.class);
        timeBlockRepository = mock(TimeBlockRepository.class);
        taskRepository = mock(TaskRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        subtaskRepository = mock(SubtaskRepository.class);
        userPreferenceRepository = mock(UserPreferenceRepository.class);
        aiSchedulingLogRepository = mock(AiSchedulingLogRepository.class);
        aiService = new AiServiceImpl(geminiApiClient, timeBlockRepository, taskRepository, categoryRepository,
                subtaskRepository, userPreferenceRepository, aiSchedulingLogRepository, mock(JsonMapper.class));
    }

    @Test
    void rejectsNullBreakdownRequest() {
        assertThrows(IllegalArgumentException.class, () -> aiService.breakdownTaskByPrompt(1L, null));
        verifyNoInteractions(geminiApiClient, taskRepository, subtaskRepository);
    }

    @Test
    void rejectsOversizedPromptFieldsBeforeCallingAi() {
        AiBreakdownRequest request = AiBreakdownRequest.builder()
                .title("Task")
                .description("x".repeat(4001))
                .durationMinutes(60)
                .build();

        assertThrows(IllegalArgumentException.class, () -> aiService.breakdownTaskByPrompt(1L, request));
        verifyNoInteractions(geminiApiClient, taskRepository, subtaskRepository);
    }

    @Test
    void rejectsInvalidDurationBeforeCallingAi() {
        AiBreakdownRequest request = AiBreakdownRequest.builder()
                .title("Task")
                .description("Description")
                .durationMinutes(1441)
                .build();

        assertThrows(IllegalArgumentException.class, () -> aiService.breakdownTaskByPrompt(1L, request));
        verifyNoInteractions(geminiApiClient, taskRepository, subtaskRepository);
    }

    @Test
    void parsesMultipleTasksInSinglePromptSuccessfully() {
        com.taskmanager.dto.request.AiParseTaskRequest request = com.taskmanager.dto.request.AiParseTaskRequest.builder()
                .userId(1L)
                .prompt("Sáng mai 8h học Tiếng Anh 1 tiếng, sau đó 10h họp nhóm đồ án 2 tiếng, chiều 15h đi bơi")
                .currentDateTime("2026-11-11T09:00:00+07:00")
                .build();

        com.taskmanager.dto.response.AiParseTaskResponse response = aiService.parseTaskFromNaturalLanguage(request);

        org.junit.jupiter.api.Assertions.assertNotNull(response);
        org.junit.jupiter.api.Assertions.assertTrue(response.getIsValidTask());
        org.junit.jupiter.api.Assertions.assertNotNull(response.getTasks());
        org.junit.jupiter.api.Assertions.assertEquals(3, response.getTasks().size());
        org.junit.jupiter.api.Assertions.assertEquals("Học Tiếng Anh", response.getTasks().get(0).getTitle());
        org.junit.jupiter.api.Assertions.assertEquals("Họp nhóm đồ án", response.getTasks().get(1).getTitle());
        org.junit.jupiter.api.Assertions.assertEquals("Đi bơi", response.getTasks().get(2).getTitle());
    }

    @Test
    void parsesCommaSeparatedTasksSuccessfully() {
        com.taskmanager.dto.request.AiParseTaskRequest request = com.taskmanager.dto.request.AiParseTaskRequest.builder()
                .userId(1L)
                .prompt("Mai 9h học tiếng anh, 14h làm đồ án, 17h tập gym")
                .currentDateTime("2026-11-11T09:00:00+07:00")
                .build();

        com.taskmanager.dto.response.AiParseTaskResponse response = aiService.parseTaskFromNaturalLanguage(request);

        org.junit.jupiter.api.Assertions.assertNotNull(response);
        org.junit.jupiter.api.Assertions.assertTrue(response.getIsValidTask());
        org.junit.jupiter.api.Assertions.assertNotNull(response.getTasks());
        org.junit.jupiter.api.Assertions.assertEquals(3, response.getTasks().size());
        org.junit.jupiter.api.Assertions.assertEquals("Học tiếng anh", response.getTasks().get(0).getTitle());
        org.junit.jupiter.api.Assertions.assertEquals("Làm đồ án", response.getTasks().get(1).getTitle());
        org.junit.jupiter.api.Assertions.assertEquals("Tập gym", response.getTasks().get(2).getTitle());
    }

    @Test
    void parsesNumberedListTasksSuccessfully() {
        com.taskmanager.dto.request.AiParseTaskRequest request = com.taskmanager.dto.request.AiParseTaskRequest.builder()
                .userId(1L)
                .prompt("Mai có 3 việc: 1. 8h đi chợ 2. 10h nấu cơm 3. 14h học bài")
                .currentDateTime("2026-11-11T09:00:00+07:00")
                .build();

        com.taskmanager.dto.response.AiParseTaskResponse response = aiService.parseTaskFromNaturalLanguage(request);

        org.junit.jupiter.api.Assertions.assertNotNull(response);
        org.junit.jupiter.api.Assertions.assertTrue(response.getIsValidTask());
        org.junit.jupiter.api.Assertions.assertNotNull(response.getTasks());
        org.junit.jupiter.api.Assertions.assertEquals(3, response.getTasks().size());
        org.junit.jupiter.api.Assertions.assertEquals("Đi chợ", response.getTasks().get(0).getTitle());
        org.junit.jupiter.api.Assertions.assertEquals("Nấu cơm", response.getTasks().get(1).getTitle());
        org.junit.jupiter.api.Assertions.assertEquals("Học bài", response.getTasks().get(2).getTitle());
    }
}
