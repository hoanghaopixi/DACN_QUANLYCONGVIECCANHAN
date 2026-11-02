package com.taskmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "caidatnguoidung")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "\"id\"")
    private Long id;

    @Column(name = "\"NguoiDungId\"", nullable = false, unique = true)
    private Long userId;

    @Column(name = "\"GioBatDauLamViec\"")
    @Builder.Default
    private LocalTime workStartTime = LocalTime.of(8, 0);

    @Column(name = "\"GioKetThucLamViec\"")
    @Builder.Default
    private LocalTime workEndTime = LocalTime.of(18, 0);

    @Column(name = "\"NgayLamViec\"", length = 50)
    @Builder.Default
    private String workDays = "MON,TUE,WED,THU,FRI";

    @Column(name = "\"ThoiGianDemPhut\"")
    @Builder.Default
    private Integer bufferMinutes = 15;

    @Column(name = "\"ThoiGianHieuSuatCao\"", length = 20)
    @Builder.Default
    private String peakProductivityTime = "MORNING"; // MORNING, AFTERNOON, EVENING

    @Column(name = "\"TuDongDoiLich\"")
    @Builder.Default
    private Boolean autoReschedule = true;

    @Column(name = "\"MuiGio\"", length = 50)
    @Builder.Default
    private String timeZone = "Asia/Ho_Chi_Minh";

    @Column(name = "\"GiaoDien\"", length = 20)
    @Builder.Default
    private String theme = "LIGHT"; // LIGHT, DARK, SYSTEM

    @Column(name = "\"HinhNenUrl\"", columnDefinition = "TEXT")
    private String backgroundUrl;

    @Column(name = "\"LoaiHinhNen\"", length = 20)
    @Builder.Default
    private String backgroundType = "NONE"; // NONE, IMAGE, VIDEO

    @Column(name = "\"DoPhuToi\"")
    @Builder.Default
    private Integer overlayDarkness = 50; // 0 to 100 percentage (default 50%)

    @Column(name = "\"NgayTao\"")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "\"NgayCapNhat\"")
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
