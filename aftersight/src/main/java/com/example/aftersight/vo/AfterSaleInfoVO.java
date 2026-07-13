package com.example.aftersight.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class AfterSaleInfoVO {
    private Integer afterSaleType;
    private String afterSaleTypeDesc;
    private String applyReason;
    private List<String> evidenceImages;
    private BigDecimal applyAmount;
}