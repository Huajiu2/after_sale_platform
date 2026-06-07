package com.example.aftersight.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
}
