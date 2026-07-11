package ro.ecoregistru.exception;

public record ParamException(
        String key,
        String error
) {}
