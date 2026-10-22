package com.taskmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "danhmuc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "\"id\"")
    private Long id;

    @Column(name = "\"NguoiDungId\"", nullable = false)
    private Long userId;

    @Column(name = "\"KhongGianId\"")
    private Long workspaceId;

    @Column(name = "\"TenDanhMuc\"", nullable = false, length = 100)
    private String name;

    @Column(name = "\"MaMau\"", length = 20)
    @Builder.Default
    private String colorCode = "#4F46E5";

    @Column(name = "\"TenBieuTuong\"", length = 50)
    @Builder.Default
    private String iconName = "Folder";

    @Column(name = "\"MacDinh\"")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "\"NgayTao\"")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "\"NgayCapNhat\"")
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
