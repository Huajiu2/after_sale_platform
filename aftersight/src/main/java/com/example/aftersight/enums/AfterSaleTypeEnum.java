package com.example.aftersight.enums;

import java.util.Arrays;

public enum AfterSaleTypeEnum {

    REFUND_ONLY(1, "仅退款"),
    REFUND_RETURN(2, "退货退款"),
    COMPLAINT(3, "投诉");

    private final int code;
    private final String desc;

    AfterSaleTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static AfterSaleTypeEnum fromCode(Integer code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(e -> e.code == code)
                .findFirst()
                .orElse(null);
    }
}
