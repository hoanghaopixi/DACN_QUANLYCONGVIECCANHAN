package com.taskmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "nguoidung")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "\"id\"")
    private Long id;

    @Column(name = "\"Email\"", nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "\"MatKhau\"", nullable = false)
    private String passwordHash;

    @Column(name = "\"HoVaTen\"", nullable = false, length = 100)
    private String fullName;

    @Column(name = "\"SoDienThoai\"", length = 20)
    private String phoneNumber;

    @Column(name = "\"AnhDaiDien\"", length = 500)
    private String avatarUrl;

    @Column(name = "\"NguonXacThuc\"", length = 20)
    @Builder.Default
    private String authProvider = "LOCAL";

    @Column(name = "\"KichHoat\"")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "\"XacThucEmail\"")
    @Builder.Default
    private Boolean isEmailVerified = false;

    @Column(name = "\"NgayTao\"")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "\"NgayCapNhat\"")
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "vaitronguoidung",
        joinColumns = @JoinColumn(name = "\"NguoiDungId\""),
        inverseJoinColumns = @JoinColumn(name = "\"VaiTroId\"")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}
