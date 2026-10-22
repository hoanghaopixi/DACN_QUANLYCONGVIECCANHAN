package com.taskmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "thongbao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "\"id\"")
    private Long id;

    @Column(name = "\"NguoiDungId\"", nullable = false)
    private Long userId;

    @Column(name = "\"CongViecId\"")
    private Long taskId;

    @Column(name = "\"LoaiThongBao\"", nullable = false, length = 50)
    @Builder.Default
    private String type = "reminder"; // reminder, ai, alert, success

    @Column(name = "\"TieuDe\"", nullable = false, length = 255)
    private String title;

    @Column(name = "\"NoiDung\"", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "\"DaDoc\"")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "\"NgayTao\"")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "\"NgayCapNhat\"")
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
