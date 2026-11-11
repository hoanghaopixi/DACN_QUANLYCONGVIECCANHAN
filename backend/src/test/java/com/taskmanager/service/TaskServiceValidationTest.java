package com.taskmanager.service;

import com.taskmanager.dto.TaskDto;
import com.taskmanager.entity.CongViec;
import com.taskmanager.repository.CategoryRepository;
import com.taskmanager.repository.SubtaskRepository;
import com.taskmanager.repository.TaskRepository;
import com.taskmanager.repository.TimeBlockRepository;
import com.taskmanager.repository.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TaskServiceValidationTest {

    private TaskRepository taskRepository;
    private TimeBlockRepository timeBlockRepository;
    private CategoryRepository categoryRepository;
    private SubtaskRepository subtaskRepository;
    private WorkspaceRepository workspaceRepository;
    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        timeBlockRepository = mock(TimeBlockRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        subtaskRepository = mock(SubtaskRepository.class);
        workspaceRepository = mock(WorkspaceRepository.class);
        taskService = new TaskService(taskRepository, timeBlockRepository, categoryRepository,
                subtaskRepository, workspaceRepository);
        when(taskRepository.findById(7L))
                .thenReturn(Optional.of(CongViec.builder().id(7L).userId(3L).title("Task").status("TODO").build()));
    }

    @Test
    void rejectsInvalidStatusBeforeWriting() {
        TaskDto update = TaskDto.builder().status("NOT_A_STATUS").build();

        assertThrows(IllegalArgumentException.class, () -> taskService.updateTask(3L, 7L, update));

        verifyNoInteractions(timeBlockRepository, categoryRepository, subtaskRepository, workspaceRepository);
    }

    @Test
    void rejectsOutOfRangeDurationBeforeWriting() {
        TaskDto update = TaskDto.builder().estimatedDurationMinutes(1441).build();

        assertThrows(IllegalArgumentException.class, () -> taskService.updateTask(3L, 7L, update));

        verifyNoInteractions(timeBlockRepository, categoryRepository, subtaskRepository, workspaceRepository);
    }
}
