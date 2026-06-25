package com.example.neeews.auth.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PasswordResetConfirmRequest {
    private String email;
    private String code;
    private String newPassword;
}
