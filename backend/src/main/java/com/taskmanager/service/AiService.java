package com.taskmanager.service;

import com.taskmanager.dto.AutoScheduleDto;
import com.taskmanager.dto.SubtaskDto;
import com.taskmanager.dto.request.AiParseTaskRequest;
import com.taskmanager.dto.response.AiParseTaskResponse;

import java.util.List;

public interface AiService {
    /**
     * Bóc tách câu lệnh ngôn ngữ tự nhiên (tiếng Việt) thành dữ liệu Task có cấu trúc
     */
    AiParseTaskResponse parseTaskFromNaturalLanguage(AiParseTaskRequest request);

    /**
     * Phân rã một công việc phức tạp thành danh sách công việc con (Subtasks) bằng Gemini AI
     */
    List<SubtaskDto> breakdownTask(Long userId, Long taskId);

    /**
     * Phân rã công việc trực tiếp từ tiêu đề & mô tả (kể cả khi chưa lưu Task vào CSDL) bằng Gemini AI
     */
    List<SubtaskDto> breakdownTaskByPrompt(Long userId, com.taskmanager.dto.request.AiBreakdownRequest request);

    /**
     * Tự động sắp xếp các công việc chưa có lịch vào các khoảng thời gian trống tối ưu (1 chạm)
     */
    AutoScheduleDto.Response autoSchedule(Long userId, AutoScheduleDto request);
}
