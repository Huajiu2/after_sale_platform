package com.example.aftersight.vo;

import lombok.Data;

@Data
public class TypeRatioVO {
    private TypeRatioItem refundOnly;
    private TypeRatioItem refundReturn;
    private TypeRatioItem complaint;
}
