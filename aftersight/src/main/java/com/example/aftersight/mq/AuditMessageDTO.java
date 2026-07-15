package com.example.aftersight.mq;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditMessageDTO {
    private String msgId;
    private String ticketNo;
    private Integer afterSaleType;
    private String applyReason;
    private String orderNo;
    private Long userId;
    private Long storeId;
    private String evidenceImages;
    private LocalDateTime timestamp;
}
