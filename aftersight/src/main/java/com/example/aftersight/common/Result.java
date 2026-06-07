package com.example.aftersight.common;

import lombok.Data;

import java.time.Instant;

@Data
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;

    public static <T>Result<T> success(T data){
        Result<T> res = new Result<>();
        res.setCode(200);
        res.setMessage("success");
        res.setData(data);
        res.setTimestamp(Instant.now().toEpochMilli());
        return res;
    }

    public static <T> Result<T> fail(Integer code, String msg) {
        Result<T> res = new Result<>();
        res.setCode(code);
        res.setMessage(msg);
        res.setTimestamp(Instant.now().toEpochMilli());
        return res;
    }
}
