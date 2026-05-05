package com.flowboard.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private static final String HTML_HI_PREFIX = "<p>Hi <strong>";
    private static final String HTML_STRONG_PREFIX = "<p><strong>";
    private final JavaMailSender mailSender;
    private final ObjectProvider<EmailService> selfProvider;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Async
    public void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(wrapInTemplate(subject, htmlBody), true);
            helper.setFrom(fromAddress);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send email to: {}", to, e);
        }
    }

    public void sendMentionNotification(String to, String mentionerName, String cardName) {
        String body = HTML_STRONG_PREFIX + mentionerName + "</strong> mentioned you in card: <strong>" + cardName + "</strong></p>"
                + "<p>Head over to FlowBoard to see what they said.</p>";
        self().sendEmail(to, "You were mentioned in " + cardName, body);
    }

    public void sendAssignmentNotification(String to, String cardName) {
        String body = "<p>You have been assigned to card: <strong>" + cardName + "</strong></p>"
                + "<p>Check your FlowBoard dashboard for details.</p>";
        self().sendEmail(to, "New Card Assigned: " + cardName, body);
    }

    public void sendWorkspaceInvitation(String to, String workspaceName, String inviterName, String role) {
        String body = HTML_STRONG_PREFIX + inviterName + "</strong> has invited you to join the workspace "
                + "<strong>" + workspaceName + "</strong> as a <strong>" + role + "</strong>.</p>"
                + "<p>Log in to FlowBoard to get started!</p>";
        self().sendEmail(to, "You've been invited to " + workspaceName, body);
    }

    public void sendBoardInvitation(String to, String boardName, String inviterName, String role) {
        String body = HTML_STRONG_PREFIX + inviterName + "</strong> has added you to the board "
                + "<strong>" + boardName + "</strong> as a <strong>" + role + "</strong>.</p>"
                + "<p>Open FlowBoard to start collaborating!</p>";
        self().sendEmail(to, "You've been added to " + boardName, body);
    }

    public void sendPremiumActivatedEmail(String to, String fullName, String planName, LocalDateTime activatedAt) {
        String body = HTML_HI_PREFIX + safe(fullName) + "</strong>,</p>"
                + "<p>Your account has been upgraded to <strong>" + safe(planName) + "</strong>.</p>"
                + "<p>You can now create unlimited workspaces and add unlimited members across your team.</p>"
                + "<p><strong>Activated at:</strong> " + formatDateTime(activatedAt) + "</p>";
        self().sendEmail(to, "Premium activated on FlowBoard", body);
    }

    public void sendInvoiceEmail(String to, InvoiceEmailDetails invoice) {
        String body = HTML_HI_PREFIX + safe(invoice.fullName()) + "</strong>,</p>"
                + "<p>Thanks for upgrading to <strong>" + safe(invoice.planName()) + "</strong>. Here is your payment receipt.</p>"
                + "<table style='width:100%;border-collapse:collapse;margin-top:18px;'>"
                + row("Plan", safe(invoice.planName()))
                + row("Amount", formatAmount(invoice.amountPaise(), invoice.currency()))
                + row("Provider", safe(invoice.providerName()))
                + row("Order ID", safe(invoice.providerOrderId()))
                + row("Payment ID", safe(invoice.providerPaymentId()))
                + row("Paid on", formatDateTime(invoice.activatedAt()))
                + "</table>"
                + "<p style='margin-top:18px;'>Keep this email as your invoice for the FlowBoard premium upgrade.</p>";
        self().sendEmail(to, "FlowBoard invoice for your premium upgrade", body);
    }

    public void sendAccountSuspendedEmail(String to, String fullName) {
        String body = HTML_HI_PREFIX + safe(fullName) + "</strong>,</p>"
                + "<p>Your FlowBoard account has been <strong>suspended</strong>.</p>"
                + "<p>If you believe this happened by mistake, please contact your platform administrator or support team for help.</p>"
                + "<p>While the suspension is active, you will not be able to sign in to FlowBoard.</p>";
        self().sendEmail(to, "Your FlowBoard account has been suspended", body);
    }

    public void sendAccountRestoredEmail(String to, String fullName) {
        String body = HTML_HI_PREFIX + safe(fullName) + "</strong>,</p>"
                + "<p>Your FlowBoard account has been <strong>restored</strong>.</p>"
                + "<p>You can sign back in and continue working with your team.</p>";
        self().sendEmail(to, "Your FlowBoard account has been restored", body);
    }

    private String wrapInTemplate(String title, String content) {
        return "<!DOCTYPE html>"
                + "<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>"
                + "<body style='margin:0;padding:0;background:#eef2ff;font-family:Segoe UI,Helvetica,Arial,sans-serif;color:#0f172a;'>"
                + "<div style='display:none;max-height:0;overflow:hidden;opacity:0;'>"
                + safe(title) + " from FlowBoard."
                + "</div>"
                + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='background:linear-gradient(180deg,#eef2ff 0%,#f8fafc 100%);margin:0;padding:24px 12px;'>"
                + "<tr><td align='center'>"
                + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='max-width:640px;'>"
                + "<tr><td style='padding-bottom:16px;text-align:center;'>"
                + "<span style='display:inline-block;padding:8px 14px;border-radius:999px;background:#e0e7ff;color:#4338ca;font-size:12px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;'>FlowBoard Notification</span>"
                + "</td></tr>"
                + "<tr><td>"
                + "<table role='presentation' width='100%' cellpadding='0' cellspacing='0' style='background:#ffffff;border:1px solid #dbe4ff;border-radius:24px;overflow:hidden;box-shadow:0 18px 50px rgba(30,41,59,0.12);'>"
                + "<tr><td style='padding:0;'>"
                + "<div style='padding:36px 40px;background:radial-gradient(circle at top left,#60a5fa 0%,#4f46e5 42%,#0f172a 100%);'>"
                + "<p style='margin:0 0 10px;color:#c7d2fe;font-size:12px;font-weight:700;letter-spacing:.1em;text-transform:uppercase;'>Stay in sync</p>"
                + "<h1 style='margin:0;color:#ffffff;font-size:30px;line-height:1.2;font-weight:800;'>FlowBoard</h1>"
                + "<p style='margin:12px 0 0;color:#dbeafe;font-size:15px;line-height:1.6;'>Work updates, invitations, and account activity in one clean summary.</p>"
                + "</div>"
                + "</td></tr>"
                + "<tr><td style='padding:36px 40px 28px;'>"
                + "<h2 style='margin:0 0 14px;color:#0f172a;font-size:24px;line-height:1.3;font-weight:800;'>" + safe(title) + "</h2>"
                + "<div style='color:#475569;font-size:15px;line-height:1.8;'>" + content + "</div>"
                + "<table role='presentation' cellpadding='0' cellspacing='0' style='margin-top:28px;'><tr><td>"
                + "<a href='" + frontendUrl + "' style='display:inline-block;padding:14px 24px;border-radius:12px;background:#2563eb;color:#ffffff;text-decoration:none;font-size:14px;font-weight:700;'>Open FlowBoard</a>"
                + "</td></tr></table>"
                + "</td></tr>"
                + "<tr><td style='padding:0 40px 30px;'>"
                + "<div style='padding:18px 20px;border-radius:16px;background:#f8fafc;border:1px solid #e2e8f0;color:#64748b;font-size:13px;line-height:1.7;'>"
                + "You are receiving this email because there was activity related to your FlowBoard account."
                + "</div>"
                + "</td></tr>"
                + "<tr><td style='padding:22px 40px;border-top:1px solid #e2e8f0;background:#f8fafc;'>"
                + "<p style='margin:0 0 6px;color:#334155;font-size:12px;font-weight:700;'>FlowBoard</p>"
                + "<p style='margin:0;color:#94a3b8;font-size:12px;line-height:1.6;'>This is an automated email, so replies are not monitored.</p>"
                + "</td></tr>"
                + "</table>"
                + "</td></tr>"
                + "<tr><td style='padding-top:14px;text-align:center;color:#94a3b8;font-size:11px;line-height:1.6;'>"
                + "FlowBoard keeps your team aligned on boards, cards, and workspace activity."
                + "</td></tr>"
                + "</table>"
                + "</td></tr></table></body></html>";
    }

    private String row(String label, String value) {
        return "<tr>"
                + "<td style='padding:10px 0;border-bottom:1px solid #e2e8f0;color:#64748b;font-size:13px;'>" + label + "</td>"
                + "<td style='padding:10px 0;border-bottom:1px solid #e2e8f0;color:#0f172a;font-size:13px;text-align:right;font-weight:600;'>" + value + "</td>"
                + "</tr>";
    }

    private String formatAmount(Integer amountPaise, String currency) {
        return safe(currency) + " " + new DecimalFormat("0.00").format(amountPaise / 100.0);
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return "-";
        }
        return value.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private EmailService self() {
        return selfProvider.getObject();
    }

    public record InvoiceEmailDetails(
            String fullName,
            String planName,
            Integer amountPaise,
            String currency,
            String providerName,
            String providerOrderId,
            String providerPaymentId,
            LocalDateTime activatedAt
    ) {
    }
}
