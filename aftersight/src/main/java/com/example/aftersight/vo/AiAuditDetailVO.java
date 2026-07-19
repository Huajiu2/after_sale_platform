package com.example.aftersight.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AiAuditDetailVO {
    private String conclusion;
    private BigDecimal confidence;
    private String reason;
    private String suggestedAction;
    private String modelName;
    private Integer latencyMs;
    private LocalDateTime auditTime;
}