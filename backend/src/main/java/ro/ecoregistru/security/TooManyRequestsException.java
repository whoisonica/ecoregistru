package ro.ecoregistru.security;

import lombok.Getter;

/**
 * Thrown by the per-email half of the rate limiting, from inside a service. Carries the seconds to
 * wait so {@code AdviceController} can put a truthful {@code Retry-After} on the response.
 */
@Getter
public class TooManyRequestsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(long retryAfterSeconds) {
        super(TooManyRequests.MESSAGE);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
