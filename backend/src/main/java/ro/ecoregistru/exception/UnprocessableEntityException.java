package ro.ecoregistru.exception;

import lombok.Getter;

@Getter
public class UnprocessableEntityException extends RuntimeException {
    private final ErrorMessageEnum errorCode;

    public UnprocessableEntityException(ErrorMessageEnum errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
