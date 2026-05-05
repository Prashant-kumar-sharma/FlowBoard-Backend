package com.flowboard.auth.repository;

import com.flowboard.auth.entity.AuthOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthOtpRepository extends JpaRepository<AuthOtp, Long> {
    Optional<AuthOtp> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, AuthOtp.Purpose purpose);
}
