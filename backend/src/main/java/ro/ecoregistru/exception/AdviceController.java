package ro.ecoregistru.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ro.ecoregistru.exception.GlobalErrorMessages.*;

/**
 * Consistent error envelope for the whole API: {error-type, error-code, error-message, params?}.
 * Ported from an earlier project's AdviceController.
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

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public Map<String, Object> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
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
