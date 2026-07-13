package com.example.aftersight.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderInfoVO {
    private String orderNo;
    private String productName;
    private String productSpec;
    private BigDecimal payAmount;
    private LocalDateTime orderTime;
    private String storeName;
}