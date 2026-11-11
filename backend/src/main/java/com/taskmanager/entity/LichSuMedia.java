package com.taskmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "lich_su_media")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichSuMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "\"id\"")
    private Long id;

    @Column(name = "\"NguoiDungId\"", nullable = false)
    private Long userId;

    @Column(name = "\"LoaiMedia\"", nullable = false, length = 30)
    private String mediaType; // 'AVATAR', 'BACKGROUND_IMAGE', 'BACKGROUND_VIDEO'

    @Column(name = "\"Url\"", nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "\"TenFile\"", length = 255)
    private String fileName;

    @Column(name = "\"KichThuoc\"")
    private Long fileSize;

    @Column(name = "\"NgayTao\"")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
