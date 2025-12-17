package com.notice.system.exception;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException() {
        super("权限不足");
    }

    public ForbiddenException(String message) {
        super(message);
    }
}

