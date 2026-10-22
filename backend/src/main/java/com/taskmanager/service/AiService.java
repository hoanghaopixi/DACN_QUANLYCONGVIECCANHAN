package com.taskmanager.service;

import com.taskmanager.dto.request.AiParseTaskRequest;
import com.taskmanager.dto.response.AiParseTaskResponse;

public interface AiService {
    /**
     * Bóc tách câu lệnh ngôn ngữ tự nhiên (tiếng Việt) thành dữ liệu Task có cấu trúc
     */
    AiParseTaskResponse parseTaskFromNaturalLanguage(AiParseTaskRequest request);
}
