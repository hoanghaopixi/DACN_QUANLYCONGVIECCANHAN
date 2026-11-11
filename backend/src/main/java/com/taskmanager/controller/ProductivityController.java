package com.taskmanager.controller;

import com.taskmanager.dto.ProductivityReportDto;
import com.taskmanager.service.ProductivityService;
import com.taskmanager.util.UserResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/productivity")
@RequiredArgsConstructor
public class ProductivityController {

    private final ProductivityService productivityService;
    private final UserResolver userResolver;

    @GetMapping("/daily")
    public ResponseEntity<ProductivityReportDto> getDailyReport(
            @RequestParam(required = false) String date,
            HttpServletRequest request) {
        Long userId = userResolver.resolveUserId(request);
        return ResponseEntity.ok(productivityService.getDailyReport(userId, date));
    }
}
