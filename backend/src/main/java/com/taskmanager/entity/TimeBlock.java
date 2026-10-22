package com.taskmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "khoithoigian")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimeBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "\"id\"")
    private Long id;

    @Column(name = "\"NguoiDungId\"", nullable = false)
    private Long userId;

    @Column(name = "\"CongViecId\"")
    private Long taskId;

    @Column(name = "\"TieuDe\"", nullable = false, length = 255)
    private String title;

    @Column(name = "\"ThoiGianBatDau\"", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "\"ThoiGianKetThuc\"", nullable = false)
    private OffsetDateTime endTime;

    @Column(name = "\"TuDongXepLich\"")
    @Builder.Default
    private Boolean isAutoScheduled = false;

    @Column(name = "\"TrangThai\"", length = 20)
    @Builder.Default
    private String status = "SCHEDULED";

    @Column(name = "\"MaMau\"", length = 20)
    private String colorCode;

    @Column(name = "\"GhiChu\"", columnDefinition = "TEXT")
    private String note;

    @Column(name = "\"NgayTao\"")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "\"NgayCapNhat\"")
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
