package com.example.aftersight.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI审核日志表 ai_audit_log
 */
@Data
public class AiAuditLog {

    @Id
    private Long id;

    /** 工单ID */
    private Long ticketId;

    /** 工单号 */
    private String ticketNo;

    /** 模型名称 */
    private String llmModel;

    /** 输入Prompt（关键字段脱敏） */
    private String llmPrompt;

    /** 大模型原始输出 */
    private String llmResponse;

    /** LLM调用耗时（毫秒） */
    private Integer llmLatencyMs;

    /** 召回切片ID列表，逗号分隔 */
    private String ragChunkIds;

    /** 各切片相似度得分，逗号分隔 */
    private String ragSimilarities;

    /** 审核结论 */
    private String auditConclusion;

    /** 置信度 */
    private BigDecimal confidence;

    /** 建议处理方案 */
    private String suggestedAction;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
