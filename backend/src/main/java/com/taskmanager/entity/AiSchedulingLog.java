package com.taskmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "nhatkyai")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiSchedulingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "\"id\"")
    private Long id;

    @Column(name = "\"NguoiDungId\"", nullable = false)
    private Long userId;

    @Column(name = "\"LoaiHanhDong\"", nullable = false, length = 50)
    private String actionType; // AUTO_SCHEDULE, TASK_BREAKDOWN, NATURAL_PARSING, RESCHEDULE_OVERDUE

    @Column(name = "\"CauLenhDauVao\"", nullable = false, columnDefinition = "TEXT")
    private String promptInput;

    @Column(name = "\"PhanHoiAI\"", columnDefinition = "jsonb")
    private String aiResponse;

    @Column(name = "\"TrangThai\"", length = 20)
    @Builder.Default
    private String status = "SUCCESS"; // SUCCESS, FAILED, PARTIALLY_APPLIED

    @Column(name = "\"SoToken\"")
    @Builder.Default
    private Integer tokenCount = 0;

    @Column(name = "\"ThoiGianXuLyMs\"")
    @Builder.Default
    private Long executionTimeMs = 0L;

    @Column(name = "\"NgayTao\"")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
