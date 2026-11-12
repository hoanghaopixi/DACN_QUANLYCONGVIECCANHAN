package com.taskmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "xacthucotp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class XacThucOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "\"id\"")
    private Long id;

    @Column(name = "\"Email\"", nullable = false, length = 120)
    private String email;

    @Column(name = "\"MaOtp\"", nullable = false, length = 10)
    private String otpCode;

    @Column(name = "\"LoaiOtp\"", length = 30)
    @Builder.Default
    private String otpType = "FORGOT_PASSWORD";

    @Column(name = "\"HetHanLuc\"", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "\"DaSuDung\"")
    @Builder.Default
    private Boolean isUsed = false;

    @Column(name = "\"SoLanThu\"")
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "\"NgayTao\"")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
