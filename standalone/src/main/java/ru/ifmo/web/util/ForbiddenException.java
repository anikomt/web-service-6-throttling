package ru.ifmo.web.util;

public class ForbiddenException extends RuntimeException {
    public static ForbiddenException DEFAULT_INSTANCE = new
            ForbiddenException("Wrong login/password");

    public ForbiddenException(String message) {
        super(message);
    }
}
