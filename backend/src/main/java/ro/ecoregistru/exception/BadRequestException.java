package ro.ecoregistru.exception;

import lombok.Getter;

@Getter
public class BadRequestException extends RuntimeException {
    private final ErrorMessageEnum errorCode;

    public BadRequestException(ErrorMessageEnum errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
