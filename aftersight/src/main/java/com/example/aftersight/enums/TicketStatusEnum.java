package com.example.aftersight.enums;

import java.util.Arrays;

public enum TicketStatusEnum {

    PENDING_AI(0, "待AI审核"),
    AI_CLOSED(1, "AI已办结"),
    PENDING_MANUAL(2, "待人工审核"),
    REJECTED(3, "已驳回"),
    CLOSED(4, "已关闭");

    private final int code;
    private final String desc;

    TicketStatusEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static TicketStatusEnum fromCode(Integer code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElse(null);
    }
}
