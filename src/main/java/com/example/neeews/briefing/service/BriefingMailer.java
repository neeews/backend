package com.example.neeews.briefing.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Component
public class BriefingMailer {

    private static final String SENDER_DISPLAY_NAME = "neeews";

    private final JavaMailSender mailSender;
    private final String senderAddress;

    // 인증 메일과 같은 계정으로 보내면, 발송 한도를 넘었을 때 회원가입·비밀번호 재설정 메일까지 막힌다.
    // 브리핑 전용 계정을 넣으면 그 계정으로 보내고, 비워 두면 기본 계정(spring.mail)을 쓴다.
    public BriefingMailer(JavaMailSender defaultSender,
                          @Value("${spring.mail.host}") String host,
                          @Value("${spring.mail.port}") int port,
                          @Value("${spring.mail.username}") String defaultUsername,
                          @Value("${app.briefing.mail.username}") String username,
                          @Value("${app.briefing.mail.password}") String password) {
        if (username.isBlank()) {
            this.mailSender = defaultSender;
            this.senderAddress = defaultUsername;
        } else {
            this.mailSender = newSender(host, port, username, password);
            this.senderAddress = username;
        }
    }

    public void send(String to, BriefingRenderer.BriefingMail mail) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(senderAddress, SENDER_DISPLAY_NAME);
            helper.setTo(to);
            helper.setSubject(mail.subject());
            helper.setText(mail.text(), mail.html());
            if (mail.unsubscribeUrl() != null) {
                // Gmail·Outlook이 메일 상단에 "구독 취소" 버튼을 띄우는 표준 헤더다(RFC 8058 원클릭 해지).
                message.setHeader("List-Unsubscribe", "<" + mail.unsubscribeUrl() + ">");
                message.setHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
            }
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new MailPreparationException("브리핑 메일을 만들지 못했습니다.", e);
        }
        mailSender.send(message);
    }

    // 기본 계정의 SMTP 옵션(spring.mail.properties)과 같게 맞춘다.
    private static JavaMailSenderImpl newSender(String host, int port, String username, String password) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        return sender;
    }
}
