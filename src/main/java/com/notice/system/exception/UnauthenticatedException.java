package com.notice.system.exception;

public class UnauthenticatedException extends RuntimeException {
    public UnauthenticatedException() {
        super("未登录");
    }

    public UnauthenticatedException(String message) {
        super(message);
    }
}

