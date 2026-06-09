package com.example.aftersight.enums;

import java.util.Arrays;

public enum AiAuditStatusEnum {

    PENDING(0, "待审核"),
    COMPLETED(1, "已办结"),
    PENDING_MANUAL(2, "待人工判定");

    private final int code;
    private final String desc;

    AiAuditStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static AiAuditStatusEnum fromCode(Integer code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElse(null);
    }
}
