package com.example.aftersight.enums;

import java.util.Arrays;

public enum DlqStatusEnum {

    PENDING(0, "待处理"),
    RETRIED(1, "已重试"),
    DELETED(2, "已删除");

    private final int code;
    private final String desc;

    DlqStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static DlqStatusEnum fromCode(Integer code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElse(null);
    }
}
