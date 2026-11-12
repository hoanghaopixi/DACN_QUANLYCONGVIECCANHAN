package com.taskmanager.repository;

import com.taskmanager.entity.XacThucOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface XacThucOtpRepository extends JpaRepository<XacThucOtp, Long> {

    Optional<XacThucOtp> findTopByEmailAndOtpTypeAndIsUsedFalseOrderByCreatedAtDesc(String email, String otpType);

    long countByEmailAndOtpTypeAndCreatedAtAfter(String email, String otpType, java.time.OffsetDateTime after);
}
