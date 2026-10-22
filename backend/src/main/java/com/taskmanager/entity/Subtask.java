package com.taskmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "congvieccon")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subtask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "\"id\"")
    private Long id;

    @Column(name = "\"CongViecId\"", nullable = false)
    private Long taskId;

    @Column(name = "\"TieuDe\"", nullable = false, length = 255)
    private String title;

    @Column(name = "\"HoanThanh\"")
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "\"ThoiLuongPhut\"")
    @Builder.Default
    private Integer durationMinutes = 15;

    @Column(name = "\"ThuTu\"")
    @Builder.Default
    private Integer orderIndex = 0;

    @Column(name = "\"NgayTao\"")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
