package com.example.aftersight.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 操作日志表 operation_log
 */
@Data
public class OperationLog {

    private Long id;

    /** 业务类型：order_audit/doc_upload/dlq_retry/config_update */
    private String bizType;

    /** 业务ID（如工单号/文档ID） */
    private String bizId;

    /** 操作人 */
    private String operator;

    /** 操作动作 */
    private String action;

    /** 操作详情（JSON格式） */
    private String detail;

    /** 操作IP地址 */
    private String ipAddress;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
