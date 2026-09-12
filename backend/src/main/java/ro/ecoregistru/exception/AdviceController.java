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
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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
     * BUG-001. Trei greşeli de client pe care Spring le aruncă înainte de orice controller: un
     * {@code @RequestParam} obligatoriu care lipseşte, un verb care nu există pe calea cerută, şi
     * un UUID stricat în cale. Fără handlerele astea cădeau toate în plasa de la urmă, adică
     * <b>500 + Sentry</b> pentru o cerere pe care serverul a înţeles-o perfect şi a respins-o
     * corect. Costul real nu era codul de răspuns, ci canalul de erori: aplicaţia are patru
     * resurse fără citire după id, deci patru căi pe care un client cinstit greşeşte verbul, iar
     * un scaner care plimbă verbe peste API umplea singur colectorul aprins pentru lansare.
     *
     * <p>Niciunul nu cheamă Sentry, şi toate trei păstrează mesajul generic — proprietatea că un
     * răspuns de eroare nu scurge numele excepţiei sau o urmă de stivă e probată separat
     * ({@code ApiErrorContractIT}) şi nu se strică aici.
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Map<String, Object> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("Missing request parameter: {}", e.getParameterName());
        return envelope(BAD_REQUEST, "request.parameter.missing",
                "Cererea nu conține toți parametrii necesari.");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Map<String, Object> handleParameterTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("Invalid request parameter: {}", e.getName());
        return envelope(BAD_REQUEST, "request.parameter.invalid",
                "Cererea conține un parametru într-un format invalid.");
    }

    /**
     * ponytail: fără antetul {@code Allow}. RFC 9110 îl cere la 405, dar niciun client al nostru
     * nu-l citeşte; se adaugă din {@code e.getSupportedMethods()} dacă apare unul care-l foloseşte.
     */
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Map<String, Object> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        log.warn("Method not allowed: {}", e.getMethod());
        return envelope(BAD_REQUEST, "request.method.not.allowed",
                "Metoda HTTP nu este permisă pe această adresă.");
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
