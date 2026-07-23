package com.example.aftersight.vo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DlqMessageVO {
    private Long id;
    private String msgId;
    private String queueName;
    private String exchangeName;
    private String routingKey;
    private String ticketNo;
    private String errorReason;
    private Integer retryCount;
    private Integer maxRetry;
    private Integer dlqStatus;
    private String dlqStatusDesc;
    private LocalDateTime errorTime;
    private LocalDateTime lastRetryTime;
}
