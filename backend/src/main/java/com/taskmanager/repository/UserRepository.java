package com.taskmanager.repository;

import com.taskmanager.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<NguoiDung, Long> {
    Optional<NguoiDung> findByEmail(String email);
    Optional<NguoiDung> findByEmailIgnoreCase(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
}
