package com.example.neeews.auth.service;

import com.example.neeews.auth.domain.EmailVerification;
import com.example.neeews.auth.dto.request.EmailSendRequest;
import com.example.neeews.auth.dto.request.EmailVerifyRequest;
import com.example.neeews.auth.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private static final String SENDER_DISPLAY_NAME = "neeews";
    // 하루 발송 횟수를 이 테이블의 행으로 세므로, 하루보다 짧게 줄이면 발송 제한이 풀린다.
    private static final int EXPIRED_RETENTION_DAYS = 1;
    // 6자리 코드는 시도 제한이 없으면 5분 안에 대입으로 뚫린다. 코드 하나당 이 횟수까지만 받는다.
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    // 코드를 새로 받으면 시도 횟수도 새로 생기므로 발송 횟수까지 묶어야 대입 총량이 막힌다(하루 최대 50번 / 100만).
    private static final int MAX_SENDS_PER_DAY = 10;

    private final EmailVerificationRepository emailVerificationRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderAddress;

    @Transactional
    public void sendVerificationCode(EmailSendRequest request) {
        sendCode(request.getEmail(), "[neeews] 이메일 인증 코드");
    }

    @Transactional
    public void sendPasswordResetCode(String email) {
        sendCode(email, "[neeews] 비밀번호 재설정 코드");
    }

    // 틀린 횟수는 예외를 던진 뒤에도 저장돼야 하므로 IllegalArgumentException으로는 롤백하지 않는다.
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void verifyCode(EmailVerifyRequest request) {
        verifyCodeDirect(request.getEmail(), request.getCode());
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void verifyCodeDirect(String email, String code) {
        EmailVerification verification = emailVerificationRepository
                .findTopByEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new IllegalArgumentException("인증 코드를 먼저 요청해주세요."));
        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("인증 코드가 만료되었습니다.");
        }
        if (verification.getFailedAttempts() >= MAX_VERIFY_ATTEMPTS) {
            throw new IllegalArgumentException("인증 시도 횟수를 초과했습니다. 인증 코드를 다시 요청해주세요.");
        }
        if (!verification.getCode().equals(code)) {
            verification.recordFailedAttempt();
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

    // consumeVerification은 가입·비밀번호 재설정이 성공했을 때만 불린다. 인증만 받고 이탈하면
    // 행이 그대로 남아 이메일이 영구 보관되므로, 만료된 건을 주기적으로 지운다.
    // 만료 직후 바로 지우면 재방문한 사용자에게 "만료되었습니다" 대신 "코드를 먼저 요청해주세요"가
    // 떠서 혼란스러우므로 하루 지난 것만 정리한다.
    @Transactional
    public long deleteExpired() {
        return emailVerificationRepository.deleteByExpiresAtBefore(
                LocalDateTime.now().minusDays(EXPIRED_RETENTION_DAYS));
    }

    private void sendCode(String email, String subject) {
        checkSendLimit(email);
        String code = generateCode();
        emailVerificationRepository.save(EmailVerification.builder()
                .email(email)
                .code(code)
                .verified(false)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build());
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(SENDER_DISPLAY_NAME + " <" + senderAddress + ">");
        message.setTo(email);
        message.setSubject(subject);
        message.setText("인증 코드: " + code + "\n\n5분 이내에 입력해주세요.");
        mailSender.send(message);
    }

    private void checkSendLimit(String email) {
        LocalDateTime now = LocalDateTime.now();
        boolean tooSoon = emailVerificationRepository.findTopByEmailOrderByCreatedAtDesc(email)
                .filter(latest -> latest.getCreatedAt().isAfter(now.minusSeconds(RESEND_COOLDOWN_SECONDS)))
                .isPresent();
        if (tooSoon) {
            throw new IllegalArgumentException("인증 코드는 1분에 한 번만 요청할 수 있습니다.");
        }
        if (emailVerificationRepository.countByEmailAndCreatedAtAfter(email, now.minusDays(1)) >= MAX_SENDS_PER_DAY) {
            throw new IllegalArgumentException("오늘 인증 코드 요청 횟수를 초과했습니다. 나중에 다시 시도해주세요.");
        }
    }

    private String generateCode() {
        return String.format("%06d", new SecureRandom().nextInt(1_000_000));
    }
}
