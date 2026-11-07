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
}
