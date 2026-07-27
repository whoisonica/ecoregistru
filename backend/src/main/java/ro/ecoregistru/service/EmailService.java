package ro.ecoregistru.service;

import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import ro.ecoregistru.entity.AppUser;
import ro.ecoregistru.exception.EmailException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static ro.ecoregistru.exception.ErrorMessageEnum.EMAIL_SEND_FAILED;

/**
 * Sends transactional emails (Romanian). Templates live under resources/templates/mail/.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EmailService {

    // Injected beans are final (part of the constructor); @Value fields must NOT be final,
    // otherwise Lombok pulls them into the constructor and Spring looks for a String bean.
    final JavaMailSender mailSender;
    final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    String from;

    @Value("${app.frontend-base-url}")
    String frontendBaseUrl;

    public void sendVerificationEmail(AppUser user, String code) {
        Context ctx = new Context(Locale.of("ro"));
        ctx.setVariable("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        ctx.setVariable("verifyUrl", frontendBaseUrl + "/verifica-email?code=" + code);
        send(user.getEmail(), "Confirmă-ți adresa de email — EcoRegistru", "mail/verify_email", ctx);
    }

    public void sendPasswordResetEmail(AppUser user, String code) {
        Context ctx = new Context(Locale.of("ro"));
        ctx.setVariable("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        ctx.setVariable("resetUrl", frontendBaseUrl + "/reseteaza-parola?code=" + code);
        send(user.getEmail(), "Resetare parolă — EcoRegistru", "mail/forgot_password", ctx);
    }

    public void send(String to, String subject, String templateName, Context context) {
        try {
            String html = templateEngine.process(templateName, context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Sent '{}' email to {}", templateName, to);
        } catch (Exception e) {
            log.error("Failed to send '{}' email to {}", templateName, to, e);
            throw new EmailException(EMAIL_SEND_FAILED);
        }
    }
}
