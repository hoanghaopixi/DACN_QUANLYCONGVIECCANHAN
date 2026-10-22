package com.taskmanager.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vaitro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "\"id\"")
    private Integer id;

    @Column(name = "\"TenVaiTro\"", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "\"MoTa\"", length = 255)
    private String description;
}
