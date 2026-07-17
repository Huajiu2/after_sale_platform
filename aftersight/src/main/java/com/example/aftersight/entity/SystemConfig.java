package com.example.aftersight.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

/**
 * 系统参数配置表 system_config
 */
@Data
public class SystemConfig {

    @Id
    private Long id;

    /** 配置分组：redis/ai_rag/mq */
    private String configGroup;

    /** 配置键 */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 配置说明 */
    private String configDesc;

    /** 值类型：string/int/boolean */
    private String valueType;

    /** 是否加密存储：0否 1是 */
    private Integer isEncrypted;

    /** 排序号 */
    private Integer sortOrder;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
