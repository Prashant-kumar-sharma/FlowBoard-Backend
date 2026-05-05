package com.flowboard.auth.service;

import jakarta.mail.Address;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthOtpEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private ObjectProvider<AuthOtpEmailService> selfProvider;

    private AuthOtpEmailService service;

    @BeforeEach
    void setUp() {
        service = new AuthOtpEmailService(mailSender, selfProvider);
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@flowboard.test");
    }

    @Test
    void sendOtpEmailDelegatesWithFormattedHtml() {
        AuthOtpEmailService self = mock(AuthOtpEmailService.class);
        when(selfProvider.getObject()).thenReturn(self);

        service.sendOtpEmail("alice@test.com", "sign in", "123456");

        verify(self).sendEmail(eq("alice@test.com"), eq("Your FlowBoard verification code"), anyString());
    }

    @Test
    void sendEmailBuildsMimeMessageWithExpectedFields() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        service.sendEmail("alice@test.com", "Subject", "<p>Hello</p>");

        verify(mailSender).send(mimeMessage);
        Address[] recipients = mimeMessage.getRecipients(MimeMessage.RecipientType.TO);
        assertThat(recipients).hasSize(1);
        assertThat(((InternetAddress) recipients[0]).getAddress()).isEqualTo("alice@test.com");
        assertThat(mimeMessage.getSubject()).isEqualTo("Subject");
        assertThat(((InternetAddress) mimeMessage.getFrom()[0]).getAddress()).isEqualTo("noreply@flowboard.test");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        mimeMessage.writeTo(output);
        assertThat(output.toString()).contains("Hello");
    }

    @Test
    void sendEmailSwallowsMailExceptions() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        org.mockito.Mockito.doThrow(new MailSendException("boom")).when(mailSender).send(mimeMessage);

        assertThatCode(() -> service.sendEmail("alice@test.com", "Subject", "<p>Hello</p>"))
                .doesNotThrowAnyException();
    }
}
