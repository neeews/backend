package com.example.neeews.auth.repository;

import com.example.neeews.auth.domain.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findTopByEmailOrderByCreatedAtDesc(String email);
    boolean existsByEmailAndVerifiedTrue(String email);
}
