package ru.practicum.ewm.errorHandler.exceptions;

public class ValidationException extends RuntimeException {
    public ValidationException(String e) {
        super(e);
    }
}
