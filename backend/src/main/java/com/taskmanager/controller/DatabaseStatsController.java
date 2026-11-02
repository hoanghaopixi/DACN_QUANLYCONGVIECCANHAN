package com.taskmanager.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class DatabaseStatsController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/table-stats")
    public Map<String, Object> getTableStats() {
        Map<String, Object> result = new LinkedHashMap<>();

        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE' ORDER BY table_name",
                String.class
        );

        Map<String, Long> counts = new LinkedHashMap<>();
        for (String table : tables) {
            try {
                Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM \"" + table + "\"", Long.class);
                counts.put(table, count != null ? count : 0L);
            } catch (Exception e) {
                counts.put(table, -1L);
            }
        }

        result.put("totalTables", tables.size());
        result.put("tables", counts);
        return result;
    }
}
