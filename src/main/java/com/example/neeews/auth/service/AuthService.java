package com.example.neeews.auth.service;

import com.example.neeews.auth.domain.RefreshToken;
import com.example.neeews.auth.domain.User;
import com.example.neeews.auth.dto.*;
import com.example.neeews.auth.repository.RefreshTokenRepository;
import com.example.neeews.auth.repository.UserRepository;
import com.example.neeews.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationService emailVerificationService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public TokenResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (!emailVerificationService.isVerified(request.getEmail())) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
        }
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        userRepository.save(user);
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        refreshTokenRepository.deleteByUser(user);
        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String refreshTokenStr) {
        if (!jwtUtil.isTokenValid(refreshTokenStr)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }
        RefreshToken stored = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 리프레시 토큰입니다."));
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(stored);
            throw new IllegalArgumentException("리프레시 토큰이 만료되었습니다.");
        }
        String email = jwtUtil.extractEmail(refreshTokenStr);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        refreshTokenRepository.delete(stored);
        return issueTokens(user);
    }

    @Transactional
    public void logout(String refreshTokenStr) {
        refreshTokenRepository.deleteByToken(refreshTokenStr);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        if (!userRepository.existsByEmail(email)) {
            return;
        }
        emailVerificationService.sendPasswordResetCode(email);
    }

    @Transactional
    public void confirmPasswordReset(PasswordResetConfirmRequest request) {
        emailVerificationService.verifyCodeDirect(request.getEmail(), request.getCode());
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    public UserResponse getMe(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return UserResponse.from(user);
    }

    private TokenResponse issueTokens(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build());
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserResponse.from(user))
                .build();
    }
}
