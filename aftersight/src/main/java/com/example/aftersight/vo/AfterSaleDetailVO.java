package com.example.aftersight.vo;

import com.example.aftersight.entity.AiAuditLog;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AfterSaleDetailVO {

    private String ticketNo;

    private OrderInfoVO orderInfo;

    private AfterSaleInfoVO afterSaleInfo;

    private List<RagEvidenceVO> ragEvidence;

    private AiAuditDetailVO aiAuditDetail;

    private Integer ticketStatus;

    private String ticketStatusDesc;

    private LocalDateTime createdAt;
}