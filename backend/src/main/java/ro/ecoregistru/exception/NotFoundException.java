package ro.ecoregistru.exception;

import lombok.Getter;

@Getter
public class NotFoundException extends RuntimeException {
    private final ErrorMessageEnum error;

    public NotFoundException(ErrorMessageEnum error) {
        super(error.getMessage());
        this.error = error;
    }
}
