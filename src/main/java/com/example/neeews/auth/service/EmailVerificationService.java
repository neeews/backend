package com.example.neeews.auth.service;

import com.example.neeews.auth.domain.EmailVerification;
import com.example.neeews.auth.dto.EmailSendRequest;
import com.example.neeews.auth.dto.EmailVerifyRequest;
import com.example.neeews.auth.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository emailVerificationRepository;
    private final JavaMailSender mailSender;

    @Transactional
    public void sendVerificationCode(EmailSendRequest request) {
        String code = generateCode();
        EmailVerification verification = EmailVerification.builder()
                .email(request.getEmail())
                .code(code)
                .verified(false)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        emailVerificationRepository.save(verification);
        sendEmail(request.getEmail(), code);
    }

    @Transactional
    public void verifyCode(EmailVerifyRequest request) {
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("인증 코드를 먼저 요청해주세요."));

        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("인증 코드가 만료되었습니다.");
        }
        if (!verification.getCode().equals(request.getCode())) {
            throw new IllegalArgumentException("인증 코드가 올바르지 않습니다.");
        }
        verification.verify();
    }

    public boolean isVerified(String email) {
        return emailVerificationRepository.existsByEmailAndVerifiedTrue(email);
    }

    private String generateCode() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }

    private void sendEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[neeews] 이메일 인증 코드");
        message.setText("인증 코드: " + code + "\n\n5분 이내에 입력해주세요.");
        mailSender.send(message);
    }
}
