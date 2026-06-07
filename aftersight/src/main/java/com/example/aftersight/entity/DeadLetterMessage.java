package com.example.aftersight.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * MQ死信消息表 dead_letter_message
 */
@Data
public class DeadLetterMessage {

    private Long id;

    /** 原始消息ID */
    private String msgId;

    /** 所属队列名称 */
    private String queueName;

    /** 交换机名称 */
    private String exchangeName;

    /** 路由键 */
    private String routingKey;

    /** 关联售后单号 */
    private String ticketNo;

    /** 异常原因 */
    private String errorReason;

    /** 异常堆栈 */
    private String errorStack;

    /** 消息原始内容（JSON） */
    private String msgContent;

    /** 已重试次数 */
    private Integer retryCount;

    /** 最大重试次数 */
    private Integer maxRetry;

    /** 状态：0待处理 1已重试 2已删除 */
    private Integer dlqStatus;

    /** 入死信时间 */
    private LocalDateTime errorTime;

    /** 最后重试时间 */
    private LocalDateTime lastRetryTime;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
