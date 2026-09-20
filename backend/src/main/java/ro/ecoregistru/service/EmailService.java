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
import ro.ecoregistru.util.LogSafe;

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

    public void sendPasswordResetEmail(AppUser user, String code) {
        Context ctx = new Context(Locale.of("ro"));
        ctx.setVariable("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        ctx.setVariable("resetUrl", frontendBaseUrl + "/reseteaza-parola?code=" + code);
        send(user.getEmail(), "Resetare parolă — WasteHouse", "mail/forgot_password", ctx);
    }

    /**
     * 15.09.2026 — invitația are mailul ei. Până atunci pleca mailul de resetare („Am primit o cerere de
     * resetare… Dacă nu tu ai făcut cererea, ignoră”), valabil 30 de minute: un client invitat îl ignora
     * sau îl deschidea a doua zi expirat. Linkul duce pe aceeași pagină, fiindcă mecanismul e același.
     */
    public void sendInviteEmail(AppUser user, String code, int validDays) {
        String organization = user.getConsultancy() != null ? user.getConsultancy().getName()
                : user.getCompany() != null ? user.getCompany().getName() : "WasteHouse";
        Context ctx = new Context(Locale.of("ro"));
        ctx.setVariable("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        ctx.setVariable("organization", organization);
        ctx.setVariable("validDays", validDays);
        ctx.setVariable("resetUrl", frontendBaseUrl + "/reseteaza-parola?code=" + code);
        send(user.getEmail(), "Invitație în WasteHouse — " + organization, "mail/invite", ctx);
    }

    public void send(String to, String subject, String templateName, Context context) {
        try {
            String html = templateEngine.process(templateName, context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(from, "WasteHouse");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            // Adresa nu intră în logurile INFO (date personale păstrate la furnizorul de loguri).
            log.info("Sent '{}' email", templateName);
        } catch (Exception e) {
            log.error("Failed to send '{}' email to {}", templateName, LogSafe.email(to), e);
            throw new EmailException(EMAIL_SEND_FAILED);
        }
    }
}
