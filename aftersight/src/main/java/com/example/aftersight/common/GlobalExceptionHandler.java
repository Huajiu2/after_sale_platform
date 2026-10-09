package com.example.aftersight.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice//全局拦截所有加了@RestController的控制器，统一异常处理
public class GlobalExceptionHandler {

    /**
     * 请求参数错误
     * @return
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Object> paramErr(IllegalArgumentException e){
        return Result.fail(400,"请求参数错误!");
    }

    /**
     * 资源不存在
     */
    @ExceptionHandler(NoSuchElementException.class)
    public Result notFound(NoSuchElementException e){
        return Result.fail(404,"请求的资源不存在");
    }

    /**
     * 兜底：所有未处理的异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Object> serverErr(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "服务器内部错误");
    }

    // 入参校验失败（Spring Validation）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Object> validErr(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Result.fail(400, msg);
    }

    @ExceptionHandler(BusinessException.class)
    public Result bizErr(BusinessException e){
        log.warn("业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(),e.getMessage());
    }

}
