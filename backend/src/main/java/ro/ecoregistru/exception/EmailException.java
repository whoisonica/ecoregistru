package ro.ecoregistru.exception;

import lombok.Getter;

@Getter
public class EmailException extends RuntimeException {
    private final ErrorMessageEnum errorCode;

    public EmailException(ErrorMessageEnum errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
