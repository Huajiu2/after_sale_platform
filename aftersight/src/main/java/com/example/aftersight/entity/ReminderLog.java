package com.example.aftersight.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 催单通知记录表 reminder_log
 */
@Data
public class ReminderLog {

    private Long id;

    /** 工单ID */
    private Long ticketId;

    /** 工单号 */
    private String ticketNo;

    /** 催单类型：1超时催单 */
    private Integer reminderType;

    /** 被通知人 */
    private String reminderTo;

    /** 通知渠道：system/email/sms */
    private String reminderChannel;

    /** 状态：0已发送 1已处理 */
    private Integer reminderStatus;

    /** 发送时间 */
    private LocalDateTime sentAt;

    /** 处理时间 */
    private LocalDateTime handledAt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
