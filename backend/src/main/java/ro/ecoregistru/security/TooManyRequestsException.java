package ro.ecoregistru.security;

import lombok.Getter;

/**
 * Thrown by the per-email half of the rate limiting, from inside a service. Carries the seconds to
 * wait so {@code AdviceController} can put a truthful {@code Retry-After} on the response.
 */
@Getter
public class TooManyRequestsException extends RuntimeException {

    private final long retryAfterSeconds;
    /** Codul din plic: cel general al limitării, sau unul care spune ce anume e ocupat. */
    private final String errorCode;

    public TooManyRequestsException(long retryAfterSeconds) {
        super(TooManyRequests.MESSAGE);
        this.retryAfterSeconds = retryAfterSeconds;
        this.errorCode = TooManyRequests.ERROR_CODE;
    }

    public TooManyRequestsException(long retryAfterSeconds, ro.ecoregistru.exception.ErrorMessageEnum error) {
        super(error.getMessage());
        this.retryAfterSeconds = retryAfterSeconds;
        this.errorCode = error.getCode();
    }
}
