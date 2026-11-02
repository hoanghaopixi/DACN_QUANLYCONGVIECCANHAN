package com.taskmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "nhatkythoiquen", uniqueConstraints = {
    @UniqueConstraint(name = "uq_nhatkythoiquen_task_date", columnNames = {"\"CongViecId\"", "\"NgayGhiNhan\""})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "\"id\"")
    private Long id;

    @Column(name = "\"NguoiDungId\"", nullable = false)
    private Long userId;

    @Column(name = "\"CongViecId\"", nullable = false)
    private Long taskId;

    @Column(name = "\"NgayGhiNhan\"", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "\"TrangThai\"", length = 20)
    @Builder.Default
    private String status = "COMPLETED"; // COMPLETED, SKIPPED

    @Column(name = "\"GhiChu\"", columnDefinition = "TEXT")
    private String note;

    @Column(name = "\"NgayTao\"")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "\"NguoiDungId\"", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "\"CongViecId\"", insertable = false, updatable = false)
    private Task task;
}
