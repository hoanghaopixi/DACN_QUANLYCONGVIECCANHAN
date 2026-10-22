package com.taskmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "phientaptrung")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PomodoroSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "\"id\"")
    private Long id;

    @Column(name = "\"NguoiDungId\"", nullable = false)
    private Long userId;

    @Column(name = "\"CongViecId\"")
    private Long taskId;

    @Column(name = "\"ThoiGianBatDau\"", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "\"ThoiGianKetThuc\"")
    private OffsetDateTime endTime;

    @Column(name = "\"ThoiLuongPhut\"", nullable = false)
    @Builder.Default
    private Integer durationMinutes = 25;

    @Column(name = "\"LoaiPhien\"", length = 20)
    @Builder.Default
    private String sessionType = "WORK"; // WORK, SHORT_BREAK, LONG_BREAK

    @Column(name = "\"HoanThanh\"")
    @Builder.Default
    private Boolean isCompleted = true;

    @Column(name = "\"GhiChu\"", columnDefinition = "TEXT")
    private String note;

    @Column(name = "\"NgayTao\"")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
