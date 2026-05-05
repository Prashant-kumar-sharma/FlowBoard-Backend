package com.flowboard.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthOtpEmailService {

    private final JavaMailSender mailSender;
    private final ObjectProvider<AuthOtpEmailService> selfProvider;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Async
    public void sendOtpEmail(String to, String flowLabel, String otp) {
        String subject = "Your FlowBoard verification code";
        String html = "<p>Your FlowBoard code for <strong>" + flowLabel + "</strong> is:</p>"
                + "<p style='font-size:32px;font-weight:800;letter-spacing:0.35em;margin:18px 0 8px;color:#0f172a;'>"
                + otp
                + "</p>"
                + "<p>This code expires soon and can only be used once.</p>";
        self().sendEmail(to, subject, wrap(subject, html));
    }

    @Async
    public void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            helper.setFrom(fromAddress);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send OTP email to {}", to, e);
        }
    }

    private String wrap(String title, String content) {
        return "<!DOCTYPE html><html><body style='margin:0;padding:24px;background:#f8fafc;font-family:Segoe UI,Arial,sans-serif;color:#0f172a;'>"
                + "<div style='max-width:560px;margin:0 auto;background:#ffffff;border:1px solid #e2e8f0;border-radius:24px;overflow:hidden;'>"
                + "<div style='padding:28px 32px;background:linear-gradient(135deg,#0369a1,#0f172a);color:#fff;'>"
                + "<p style='margin:0 0 8px;font-size:12px;letter-spacing:.14em;text-transform:uppercase;color:#bae6fd;'>FlowBoard Security</p>"
                + "<h1 style='margin:0;font-size:28px;'>" + title + "</h1>"
                + "</div>"
                + "<div style='padding:32px;color:#475569;font-size:15px;line-height:1.7;'>" + content + "</div>"
                + "<div style='padding:0 32px 28px;color:#94a3b8;font-size:12px;'>This is an automated security email. Replies are not monitored.</div>"
                + "</div></body></html>";
    }

    private AuthOtpEmailService self() {
        return selfProvider.getObject();
    }
}
