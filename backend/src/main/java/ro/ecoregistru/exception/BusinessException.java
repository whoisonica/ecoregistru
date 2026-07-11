package ro.ecoregistru.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorMessageEnum errorCode;

    public BusinessException(ErrorMessageEnum errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
