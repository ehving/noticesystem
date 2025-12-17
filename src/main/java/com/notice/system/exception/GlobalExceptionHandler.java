package com.notice.system.exception;

import com.notice.system.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthenticatedException.class)
    public Result<?> handleUnauthenticated(UnauthenticatedException e) {
        log.info("未登录异常: {}", e.getMessage());
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public Result<?> handleForbidden(ForbiddenException e) {
        log.info("权限异常: {}", e.getMessage());
        return Result.fail(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleOther(Exception e) {
        log.error("未处理异常: ", e);
        return Result.fail("服务器内部错误：" + e.getMessage());
    }
}


