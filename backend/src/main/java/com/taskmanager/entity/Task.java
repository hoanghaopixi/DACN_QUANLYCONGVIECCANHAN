package com.taskmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "congviec")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "\"id\"")
    private Long id;

    @Column(name = "\"NguoiDungId\"", nullable = false)
    private Long userId;

    @Column(name = "\"KhongGianId\"")
    private Long workspaceId;

    @Column(name = "\"DanhMucId\"")
    private Long categoryId;

    @Column(name = "\"TieuDe\"", nullable = false, length = 255)
    private String title;

    @Column(name = "\"MoTa\"", columnDefinition = "TEXT")
    private String description;

    @Column(name = "\"GhiChu\"", columnDefinition = "TEXT")
    private String note;

    @Column(name = "\"MucDoUuTien\"", length = 20)
    @Builder.Default
    private String priority = "MEDIUM"; // LOW, MEDIUM, HIGH, URGENT

    @Column(name = "\"GocPhanTu\"", length = 20)
    @Builder.Default
    private String eisenhowerQuadrant = "SCHEDULE"; // DO_FIRST, SCHEDULE, DELEGATE, DONT_DO

    @Column(name = "\"TrangThai\"", length = 20)
    @Builder.Default
    private String status = "TODO"; // TODO, IN_PROGRESS, COMPLETED, CANCELLED, OVERDUE

    @Column(name = "\"HanChot\"")
    private OffsetDateTime dueDate;

    @Column(name = "\"ThoiLuongUocTinh\"")
    @Builder.Default
    private Integer estimatedDurationMinutes = 60;

    @Column(name = "\"ThoiLuongThucTe\"")
    @Builder.Default
    private Integer actualDurationMinutes = 0;

    @Column(name = "\"LichCoDinh\"")
    @Builder.Default
    private Boolean isFixedTime = false;

    @Column(name = "\"BatDauCoDinh\"")
    private OffsetDateTime fixedStartTime;

    @Column(name = "\"KetThucCoDinh\"")
    private OffsetDateTime fixedEndTime;

    @Column(name = "\"LapLai\"")
    @Builder.Default
    private Boolean isRecurring = false;

    @Column(name = "\"QuyTacLapLai\"", length = 100)
    private String recurrenceRule;

    @Column(name = "\"CongViecChaId\"")
    private Long parentTaskId;

    @Column(name = "\"HoanThanhLuc\"")
    private OffsetDateTime completedAt;

    @Column(name = "\"NgayTao\"")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "\"NgayCapNhat\"")
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
