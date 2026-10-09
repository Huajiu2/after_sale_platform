package com.example.aftersight.common;

import lombok.Getter;

/**
 * 自定义业务异常类
 */
@Getter
public class BusinessException extends RuntimeException{
    private final Integer code;
    public BusinessException(Integer code,String message){
        super(message);//调用父类的构造方法
        this.code=code;
    }

    public static BusinessException notFound(String msg){
        return new BusinessException(404,msg);
    }
    public static BusinessException badRequest(String msg) {
        return new BusinessException(400, msg);
    }
    public static BusinessException serverError(String msg) {
        return new BusinessException(500, msg);
    }
}
