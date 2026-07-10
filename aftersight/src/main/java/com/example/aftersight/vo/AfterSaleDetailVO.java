package com.example.aftersight.vo;

import com.example.aftersight.entity.AfterSaleOrder;
import com.example.aftersight.entity.AiAuditLog;
import com.example.aftersight.entity.OrderInfo;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AfterSaleDetailVO {

    private String ticketNo;

    private OrderInfo orderInfo;

    private AfterSaleOrder afterSaleInfo;

    private List<RagEvidenceVO> ragEvidence;

    private AiAuditLog aiAuditDetail;

    private Integer ticketStatus;

    private String ticketStatusDesc;

    private LocalDateTime createdAt;
}