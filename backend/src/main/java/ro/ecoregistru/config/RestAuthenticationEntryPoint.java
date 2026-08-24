package ro.ecoregistru.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static ro.ecoregistru.exception.GlobalErrorMessages.ERROR_CODE;
import static ro.ecoregistru.exception.GlobalErrorMessages.ERROR_MESSAGE;
import static ro.ecoregistru.exception.GlobalErrorMessages.ERROR_TYPE;
import static ro.ecoregistru.exception.GlobalErrorMessages.UNAUTHORIZED;

/**
 * Answers an unauthenticated request with **401**, in the same envelope as the rest of the API.
 *
 * <p>Without it Spring Security falls back to {@code Http403ForbiddenEntryPoint}, and a session
 * that had simply expired came back indistinguishable from "you are logged in but not allowed
 * here". The frontend cannot act on that: it has to keep the user where they are on a 403, and
 * send them to the login page on a 401. Until 24.08.2026 it received 403 for both, which is why
 * an expired session ended in a silent, unexplained bounce.
 *
 * <p>403 stays what it always was — {@code AccessDeniedException} for an authenticated user
 * reaching past their role.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), Map.of(
                ERROR_TYPE, UNAUTHORIZED,
                ERROR_CODE, "session.expired",
                ERROR_MESSAGE, "Sesiunea a expirat sau nu ești autentificat. Autentifică-te din nou."));
    }
}
