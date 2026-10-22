package com.taskmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "nhatkynangsuat", uniqueConstraints = {
    @UniqueConstraint(name = "uq_nhatkynangsuat_ngaybaocao", columnNames = {"\"NguoiDungId\"", "\"NgayBaoCao\""})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "\"id\"")
    private Long id;

    @Column(name = "\"NguoiDungId\"", nullable = false)
    private Long userId;

    @Column(name = "\"NgayBaoCao\"", nullable = false)
    private LocalDate reportDate;

    @Column(name = "\"TongCongViecHoanThanh\"")
    @Builder.Default
    private Integer totalCompletedTasks = 0;

    @Column(name = "\"TongPhutTapTrung\"")
    @Builder.Default
    private Integer totalFocusMinutes = 0;

    @Column(name = "\"TyLeDungHan\"", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal onTimeRate = BigDecimal.ZERO;

    @Column(name = "\"SoCongViecQuaHan\"")
    @Builder.Default
    private Integer overdueTaskCount = 0;

    @Column(name = "\"NhanXetAI\"", columnDefinition = "TEXT")
    private String aiFeedback;

    @Column(name = "\"NgayTao\"")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
