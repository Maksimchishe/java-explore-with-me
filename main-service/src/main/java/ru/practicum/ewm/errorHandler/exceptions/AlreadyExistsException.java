package ru.practicum.ewm.errorHandler.exceptions;

public class AlreadyExistsException extends RuntimeException {
    public AlreadyExistsException(String e) {
        super(e);
    }
}
