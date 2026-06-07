package com.example.aftersight.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后工单表 after_sale_order（核心表）
 */
@Data
public class AfterSaleOrder {

    private Long id;

    /** 工单号，格式 SH+yyyyMMdd+3位序列 */
    private String ticketNo;

    /** 关联订单ID */
    private Long orderId;

    /** 关联订单号 */
    private String orderNo;

    /** 店铺ID */
    private Long storeId;

    /** 用户ID */
    private Long userId;

    /** 售后类型：1仅退款 2退货退款 3投诉 */
    private Integer afterSaleType;

    /** 申请原因 */
    private String applyReason;

    /** 凭证图片URL列表，JSON数组 */
    private String evidenceImages;

    /** 申请退款金额 */
    private BigDecimal applyAmount;

    /** AI审核结论 */
    private String aiAuditResult;

    /** AI置信度 0.00-100.00 */
    private BigDecimal aiConfidence;

    /** AI审核状态：0待审核 1已办结 2待人工判定 */
    private Integer aiAuditStatus;

    /** AI审核完成时间 */
    private LocalDateTime aiAuditTime;

    /** 工单状态：0待AI审核 1AI已办结 2待人工审核 3已驳回 4已关闭 */
    private Integer ticketStatus;

    /** 人工审核人 */
    private String manualAuditBy;

    /** 人工审核时间 */
    private LocalDateTime manualAuditTime;

    /** 人工备注 */
    private String manualRemark;

    /** 人工审核结果：1同意售后 2驳回售后 */
    private Integer manualResult;

    /** RAG召回规则ID列表，逗号分隔 */
    private String ragRuleIds;

    /** MQ消息ID（用于死信追踪） */
    private String mqMsgId;

    /** MQ重试次数 */
    private Integer retryCount;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
