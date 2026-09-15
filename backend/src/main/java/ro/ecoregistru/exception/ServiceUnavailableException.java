package ro.ecoregistru.exception;

import lombok.Getter;

/**
 * An outside service we depend on did not answer. Not our defect and not the user's, so it is neither
 * a 400 nor a 500 that reaches Sentry: the user is told to type the data by hand or to try again.
 */
@Getter
public class ServiceUnavailableException extends RuntimeException {
    private final ErrorMessageEnum errorCode;

    public ServiceUnavailableException(ErrorMessageEnum errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
