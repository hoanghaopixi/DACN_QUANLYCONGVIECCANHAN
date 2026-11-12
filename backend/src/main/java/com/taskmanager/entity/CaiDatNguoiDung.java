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
public class CaiDatNguoiDung {

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

    @Column(name = "\"GioNghiTruaBatDau\"")
    @Builder.Default
    private LocalTime lunchBreakStart = LocalTime.of(12, 0);

    @Column(name = "\"GioNghiTruaKetThuc\"")
    @Builder.Default
    private LocalTime lunchBreakEnd = LocalTime.of(13, 0);

    @Column(name = "\"ChoPhepCuoiTuan\"")
    @Builder.Default
    private Boolean allowWeekendTasks = true;

    @Column(name = "\"TuDongTimKhoangTrong\"")
    @Builder.Default
    private Boolean aiAutoFindSlot = true;

    @Column(name = "\"LuonHoiTruocKhiDoi\"")
    @Builder.Default
    private Boolean aiAlwaysAskBeforeChange = true;

    @Column(name = "\"ChoPhepChiaNhoViec\"")
    @Builder.Default
    private Boolean aiAllowSplitLongTasks = true;

    @Column(name = "\"TuDongDoiLichCoDinh\"")
    @Builder.Default
    private Boolean aiAutoMoveFixedTasks = false;

    @Column(name = "\"NhacNhoTruocPhut\"")
    @Builder.Default
    private Integer reminderMinutesBefore = 15;

    @Column(name = "\"GioKhongLamPhienBatDau\"")
    @Builder.Default
    private LocalTime dndStartTime = LocalTime.of(22, 0);

    @Column(name = "\"GioKhongLamPhienKetThuc\"")
    @Builder.Default
    private LocalTime dndEndTime = LocalTime.of(7, 0);

    @Column(name = "\"ThongBaoThietBi\"")
    @Builder.Default
    private Boolean enableDeviceNotifications = true;

    @Column(name = "\"EmailTomTatBuoiSang\"")
    @Builder.Default
    private Boolean enableMorningSummaryEmail = false;

    @Column(name = "\"NgonNgu\"", length = 50)
    @Builder.Default
    private String language = "Tiếng Việt";

    @Column(name = "\"NgayTao\"")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "\"NgayCapNhat\"")
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
