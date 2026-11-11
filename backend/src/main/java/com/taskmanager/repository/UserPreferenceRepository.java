package com.taskmanager.repository;

import com.taskmanager.entity.CaiDatNguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepository<CaiDatNguoiDung, Long> {
    Optional<CaiDatNguoiDung> findByUserId(Long userId);
}
