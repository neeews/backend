package com.example.neeews.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.ROLE_USER;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder.Default
    @Column(nullable = false)
    private boolean briefingSubscribed = false;

    // 메일의 수신 거부 링크에 넣는다. 로그인 없이 누르는 링크라 추측할 수 없는 값으로 사용자를 가리킨다.
    @Column(length = 36, unique = true)
    private String briefingToken;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    // 다시 신청해도 토큰은 그대로 둔다. 예전 메일의 수신 거부 링크도 계속 통해야 한다.
    public void subscribeBriefing() {
        this.briefingSubscribed = true;
        if (this.briefingToken == null) {
            this.briefingToken = UUID.randomUUID().toString();
        }
    }

    public void unsubscribeBriefing() {
        this.briefingSubscribed = false;
    }
}
