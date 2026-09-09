package ro.ecoregistru.exception;

import io.sentry.Sentry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import ro.ecoregistru.security.TooManyRequests;
import ro.ecoregistru.security.TooManyRequestsException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ro.ecoregistru.exception.GlobalErrorMessages.*;

/**
 * Consistent error envelope for the whole API: {error-type, error-code, error-message, params?}.
 */
@Slf4j
@RestControllerAdvice
public class AdviceController {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public Map<String, Object> handleNotFound(NotFoundException e) {
        log.warn("{} {}", ERROR, e.getError().getCode());
        return envelope(NOT_FOUND, e.getError().getCode(), e.getError().getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BadRequestException.class)
    public Map<String, Object> handleBadRequest(BadRequestException e) {
        log.warn("{} {}", ERROR, e.getErrorCode().getCode());
        return envelope(BAD_REQUEST, e.getErrorCode().getCode(), e.getErrorCode().getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BusinessException.class)
    public Map<String, Object> handleBusiness(BusinessException e) {
        log.warn("{} {}", ERROR, e.getErrorCode().getCode());
        return envelope(BUSINESS_ERROR, e.getErrorCode().getCode(), e.getErrorCode().getMessage());
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(UnprocessableEntityException.class)
    public Map<String, Object> handleUnprocessable(UnprocessableEntityException e) {
        log.warn("{} {}", ERROR, e.getErrorCode().getCode());
        return envelope(UNPROCESSABLE_ENTITY, e.getErrorCode().getCode(), e.getErrorCode().getMessage());
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public Map<String, Object> handleAccessDenied(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        return envelope(ACCESS_DENIED, "access.denied", "Nu ai acces la această resursă.");
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public Map<String, Object> handleOptimisticLock(OptimisticLockingFailureException e) {
        log.warn("Concurrent update conflict: {}", e.getMessage());
        return envelope(BUSINESS_ERROR, "concurrent.update",
                "Operațiunea s-a suprapus cu o alta. Te rugăm să încerci din nou.");
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, Object> handleValidation(MethodArgumentNotValidException e) {
        Map<String, Object> errors = envelope(UNPROCESSABLE_ENTITY, BAD_REQUEST, null);
        StringBuilder message = new StringBuilder();
        List<ParamException> params = new ArrayList<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            message.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ");
            params.add(new ParamException(error.getField(), error.getDefaultMessage()));
        }
        errors.put(ERROR_MESSAGE, message.toString());
        errors.put(PARAMS, params);
        return errors;
    }

    /**
     * Malformed request body: bad JSON, or an unknown enum constant (e.g. the frontend
     * sends operationCode "X9"). Jackson raises this before validation runs, so without
     * this handler it would fall through to the generic 500. Return 400 instead.
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Map<String, Object> handleUnreadable(HttpMessageNotReadableException e) {
        log.warn("Malformed request body: {}", e.getMostSpecificCause().getMessage());
        return envelope(BAD_REQUEST, "request.malformed",
                "Cererea conține date invalide sau un cod necunoscut.");
    }

    /**
     * Plasa de sub verificarea de mărime din {@code WasteMovementService}, nu în locul ei.
     *
     * <p>Serviciul respinge orice trece de 10 MB — limita reală, cea a contului Cloudinary. Limita
     * de multipart din {@code application.yml} stă puțin deasupra ei, ca fișierul să apuce să
     * ajungă la verificarea noastră și să primească mesajul care spune cifra. Excepția asta se
     * aprinde doar pentru ce e atât de mare încât Spring îl oprește înainte de orice controller —
     * și fără handler ar fi ieșit un 500 raportat la Sentry ca defect, când e o cerere greșită.
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Map<String, Object> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        log.warn("{} {}", ERROR, ErrorMessageEnum.ATTACHMENT_TOO_LARGE.getCode());
        return envelope(BAD_REQUEST, ErrorMessageEnum.ATTACHMENT_TOO_LARGE.getCode(),
                ErrorMessageEnum.ATTACHMENT_TOO_LARGE.getMessage());
    }

    /**
     * P0.3, the per-email half. The filter writes its own 429 (it runs before any controller), so
     * this is only for the limits a service enforces — same envelope, same {@code Retry-After}.
     */
    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<Map<String, Object>> handleTooManyRequests(TooManyRequestsException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(e.getRetryAfterSeconds()))
                .body(envelope("too-many-requests", TooManyRequests.ERROR_CODE, e.getMessage()));
    }

    /**
     * P0.6 — the one branch that means „we did not expect this", and so the only one worth waking
     * anyone up for. Everything above is a normal answer to a wrong request: a CUI that does not
     * exist, a form filled in badly, a movement that breaks a rule. Reporting those too would have
     * buried the 500s under a client's typos within a week — which is why Sentry's own automatic
     * resolver is ordered out of the way in {@code application.yml} and the report is made here,
     * explicitly.
     *
     * <p>With no {@code SENTRY_DSN} set this call is a no-op, so dev and the tests are unchanged.
     * Until now the log was the whole story, and {@code ErrorBoundary.tsx} said so in as many
     * words: „Consola e tot ce avem." True while the only user was the person who wrote it.
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Map<String, Object> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        Sentry.captureException(e);
        return envelope(INTERNAL_SERVER_ERROR, "internal.error",
                "A apărut o eroare neașteptată. Te rugăm să încerci din nou.");
    }

    private Map<String, Object> envelope(String type, String code, String message) {
        Map<String, Object> errors = new HashMap<>();
        errors.put(ERROR_TYPE, type);
        errors.put(ERROR_CODE, code);
        errors.put(ERROR_MESSAGE, message);
        return errors;
    }
}
