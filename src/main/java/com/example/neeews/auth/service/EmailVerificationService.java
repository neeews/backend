package com.example.neeews.auth.service;

import com.example.neeews.auth.domain.EmailVerification;
import com.example.neeews.auth.dto.request.EmailSendRequest;
import com.example.neeews.auth.dto.request.EmailVerifyRequest;
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
        sendCode(request.getEmail(), "[neeews] 이메일 인증 코드");
    }

    @Transactional
    public void sendPasswordResetCode(String email) {
        sendCode(email, "[neeews] 비밀번호 재설정 코드");
    }

    @Transactional
    public void verifyCode(EmailVerifyRequest request) {
        verifyCodeDirect(request.getEmail(), request.getCode());
    }

    @Transactional
    public void verifyCodeDirect(String email, String code) {
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("인증 코드를 먼저 요청해주세요."));
        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("인증 코드가 만료되었습니다.");
        }
        if (!verification.getCode().equals(code)) {
            throw new IllegalArgumentException("인증 코드가 올바르지 않습니다.");
        }
        verification.verify();
    }

    public boolean isVerified(String email) {
        return emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .filter(EmailVerification::isVerified)
                .filter(v -> v.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    @Transactional
    public void consumeVerification(String email) {
        emailVerificationRepository.deleteByEmail(email);
    }

    private void sendCode(String email, String subject) {
        String code = generateCode();
        emailVerificationRepository.save(EmailVerification.builder()
                .email(email)
                .code(code)
                .verified(false)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build());
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText("인증 코드: " + code + "\n\n5분 이내에 입력해주세요.");
        mailSender.send(message);
    }

    private String generateCode() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }
}
