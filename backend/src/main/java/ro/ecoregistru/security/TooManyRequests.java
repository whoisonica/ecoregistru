package ro.ecoregistru.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

/**
 * The one shape of a 429 in this API.
 *
 * <p>The filter writes it straight to the response (it runs before any controller, so
 * {@code AdviceController} never sees it) and the service throws
 * {@link TooManyRequestsException}, which the advice turns into the same body. Two ways in, one
 * envelope — {@code {error-type, error-code, error-message}}, like every other error the frontend
 * reads, plus the {@code Retry-After} the client is owed.
 */
public final class TooManyRequests {

    public static final String ERROR_CODE = "too.many.requests";
    public static final String MESSAGE =
            "Prea multe încercări. Te rugăm să încerci din nou peste câteva minute.";

    private TooManyRequests() {
    }

    static void write(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
                {"error-type":"too-many-requests","error-code":"%s","error-message":"%s"}"""
                .formatted(ERROR_CODE, MESSAGE));
    }
}
