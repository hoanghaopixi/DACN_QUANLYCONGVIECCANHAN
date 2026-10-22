package com.taskmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "nhacnho")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "\"id\"")
    private Long id;

    @Column(name = "\"NguoiDungId\"", nullable = false)
    private Long userId;

    @Column(name = "\"CongViecId\"", nullable = false)
    private Long taskId;

    @Column(name = "\"ThoiGianNhac\"", nullable = false)
    private OffsetDateTime remindAt;

    @Column(name = "\"KenhNhac\"", length = 20)
    @Builder.Default
    private String channel = "PUSH"; // PUSH, EMAIL, IN_APP

    @Column(name = "\"DaGui\"")
    @Builder.Default
    private Boolean isSent = false;

    @Column(name = "\"GuiLuc\"")
    private OffsetDateTime sentAt;

    @Column(name = "\"NgayTao\"")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
