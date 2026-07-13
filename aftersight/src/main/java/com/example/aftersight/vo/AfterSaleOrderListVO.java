package com.example.aftersight.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AfterSaleOrderListVO {
    private String ticketNo;
    private String orderNo;
    private Long storeId;
    private String storeName;
    private Integer afterSaleType;
    private String afterSaleTypeDesc;
    private String applyReason;
    private Boolean hasEvidence;
    private String aiAuditResult;
    private BigDecimal aiConfidence;
    private Integer ticketStatus;
    private String ticketStatusDesc;
    private LocalDateTime createdAt;
}