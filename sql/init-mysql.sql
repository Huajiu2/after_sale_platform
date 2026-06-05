-- ============================================================
-- 电商智能售后中台 · MySQL 初始化脚本
-- 包含：建库、建表、索引、种子数据
-- 适用版本：MySQL 8.0+
-- ============================================================

-- 建库
CREATE DATABASE IF NOT EXISTS `after_sale_platform`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `after_sale_platform`;

-- ============================================================
-- 1. 店铺信息表
-- ============================================================
DROP TABLE IF EXISTS `store_info`;
CREATE TABLE `store_info` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `store_code`    VARCHAR(32)     NOT NULL COMMENT '店铺编码',
    `store_name`    VARCHAR(128)    NOT NULL COMMENT '店铺名称',
    `category`      VARCHAR(32)     NOT NULL COMMENT '经营类目：数码/服饰/生鲜/美妆/家电/母婴/运动/图书',
    `contact_name`  VARCHAR(32)     DEFAULT '' COMMENT '联系人',
    `contact_phone` VARCHAR(20)     DEFAULT '' COMMENT '联系电话',
    `status`        TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_store_code` (`store_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='店铺信息表';

-- ============================================================
-- 2. 订单信息表
-- ============================================================
DROP TABLE IF EXISTS `order_info`;
CREATE TABLE `order_info` (
    `id`            BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `order_no`      VARCHAR(32)     NOT NULL COMMENT '订单号',
    `store_id`      BIGINT          NOT NULL COMMENT '所属店铺ID',
    `user_id`       BIGINT          NOT NULL COMMENT '用户ID',
    `user_name`     VARCHAR(64)     DEFAULT '' COMMENT '用户名',
    `user_phone`    VARCHAR(20)     DEFAULT '' COMMENT '用户手机号',
    `product_name`  VARCHAR(256)    NOT NULL COMMENT '商品名称',
    `product_spec`  VARCHAR(128)    DEFAULT '' COMMENT '商品规格',
    `product_image` VARCHAR(512)    DEFAULT '' COMMENT '商品图片URL',
    `pay_amount`    DECIMAL(12,2)   NOT NULL COMMENT '实付金额',
    `order_status`  TINYINT         NOT NULL COMMENT '订单状态：1待发货 2已发货 3已签收 4已完成',
    `order_time`    DATETIME        NOT NULL COMMENT '下单时间',
    `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_store_id` (`store_id`),
    KEY `idx_user_phone` (`user_phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单信息表';

-- ============================================================
-- 3. 售后工单表（核心表）
-- ============================================================
DROP TABLE IF EXISTS `after_sale_order`;
CREATE TABLE `after_sale_order` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_no`         VARCHAR(32)     NOT NULL COMMENT '工单号，格式 SH+yyyyMMdd+3位序列',

    -- 关联信息
    `order_id`          BIGINT          NOT NULL COMMENT '关联订单ID',
    `order_no`          VARCHAR(32)     NOT NULL COMMENT '关联订单号',
    `store_id`          BIGINT          NOT NULL COMMENT '店铺ID',
    `user_id`           BIGINT          NOT NULL COMMENT '用户ID',

    -- 售后内容
    `after_sale_type`   TINYINT         NOT NULL COMMENT '售后类型：1仅退款 2退货退款 3投诉',
    `apply_reason`      VARCHAR(1024)   NOT NULL COMMENT '申请原因',
    `evidence_images`   VARCHAR(2048)   DEFAULT '' COMMENT '凭证图片URL列表，JSON数组',
    `apply_amount`      DECIMAL(12,2)   DEFAULT 0.00 COMMENT '申请退款金额',

    -- AI 审核
    `ai_audit_result`   VARCHAR(256)    DEFAULT '' COMMENT 'AI审核结论',
    `ai_confidence`     DECIMAL(5,2)    DEFAULT 0.00 COMMENT 'AI置信度 0.00-100.00',
    `ai_audit_status`   TINYINT         NOT NULL DEFAULT 0 COMMENT 'AI审核状态：0待审核 1已办结 2待人工判定',
    `ai_audit_time`     DATETIME        DEFAULT NULL COMMENT 'AI审核完成时间',

    -- 工单状态
    `ticket_status`     TINYINT         NOT NULL DEFAULT 0 COMMENT '工单状态：0待AI审核 1AI已办结 2待人工审核 3已驳回 4已关闭',
    `manual_audit_by`   VARCHAR(64)     DEFAULT '' COMMENT '人工审核人',
    `manual_audit_time` DATETIME        DEFAULT NULL COMMENT '人工审核时间',
    `manual_remark`     VARCHAR(1024)   DEFAULT '' COMMENT '人工备注',
    `manual_result`     TINYINT         DEFAULT NULL COMMENT '人工审核结果：1同意售后 2驳回售后',

    -- 链路追踪
    `rag_rule_ids`      VARCHAR(256)    DEFAULT '' COMMENT 'RAG召回规则ID列表，逗号分隔',
    `mq_msg_id`         VARCHAR(64)     DEFAULT '' COMMENT 'MQ消息ID（用于死信追踪）',
    `retry_count`       INT             NOT NULL DEFAULT 0 COMMENT 'MQ重试次数',

    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ticket_no` (`ticket_no`),
    KEY `idx_order_no` (`order_no`),
    KEY `idx_store_id` (`store_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_ticket_status` (`ticket_status`),
    KEY `idx_created_at` (`created_at`),
    KEY `idx_ai_audit_status` (`ai_audit_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后工单表';

-- ============================================================
-- 4. AI 审核日志表
-- ============================================================
DROP TABLE IF EXISTS `ai_audit_log`;
CREATE TABLE `ai_audit_log` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_id`         BIGINT          NOT NULL COMMENT '工单ID',
    `ticket_no`         VARCHAR(32)     NOT NULL COMMENT '工单号',

    -- LLM 调用
    `llm_model`         VARCHAR(64)     NOT NULL COMMENT '模型名称',
    `llm_prompt`        TEXT            COMMENT '输入Prompt（关键字段脱敏）',
    `llm_response`      TEXT            COMMENT '大模型原始输出',
    `llm_latency_ms`    INT             DEFAULT 0 COMMENT 'LLM调用耗时（毫秒）',

    -- RAG 检索
    `rag_chunk_ids`     VARCHAR(512)    DEFAULT '' COMMENT '召回切片ID列表，逗号分隔',
    `rag_similarities`  VARCHAR(256)    DEFAULT '' COMMENT '各切片相似度得分，逗号分隔',

    -- 结果
    `audit_conclusion`  VARCHAR(512)    NOT NULL COMMENT '审核结论',
    `confidence`        DECIMAL(5,2)    NOT NULL COMMENT '置信度',
    `suggested_action`  VARCHAR(256)    DEFAULT '' COMMENT '建议处理方案',

    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (`id`),
    KEY `idx_ticket_id` (`ticket_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI审核日志表';

-- ============================================================
-- 5. MQ 死信消息表
-- ============================================================
DROP TABLE IF EXISTS `dead_letter_message`;
CREATE TABLE `dead_letter_message` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `msg_id`            VARCHAR(64)     NOT NULL COMMENT '原始消息ID',
    `queue_name`        VARCHAR(128)    NOT NULL COMMENT '所属队列名称',
    `exchange_name`     VARCHAR(128)    DEFAULT '' COMMENT '交换机名称',
    `routing_key`       VARCHAR(128)    DEFAULT '' COMMENT '路由键',

    -- 关联业务
    `ticket_no`         VARCHAR(32)     DEFAULT '' COMMENT '关联售后单号',

    -- 异常信息
    `error_reason`      VARCHAR(1024)   NOT NULL COMMENT '异常原因',
    `error_stack`       TEXT            COMMENT '异常堆栈',
    `msg_content`       TEXT            COMMENT '消息原始内容（JSON）',

    -- 处理状态
    `retry_count`       INT             NOT NULL DEFAULT 0 COMMENT '已重试次数',
    `max_retry`         INT             NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    `dlq_status`        TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0待处理 1已重试 2已删除',

    `error_time`        DATETIME        NOT NULL COMMENT '入死信时间',
    `last_retry_time`   DATETIME        DEFAULT NULL COMMENT '最后重试时间',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_msg_id` (`msg_id`),
    KEY `idx_ticket_no` (`ticket_no`),
    KEY `idx_dlq_status` (`dlq_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ死信消息表';

-- ============================================================
-- 6. 系统参数配置表
-- ============================================================
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `config_group`      VARCHAR(64)     NOT NULL COMMENT '配置分组：redis/ai_rag/mq',
    `config_key`        VARCHAR(128)    NOT NULL COMMENT '配置键',
    `config_value`      VARCHAR(512)    NOT NULL COMMENT '配置值',
    `config_desc`       VARCHAR(256)    DEFAULT '' COMMENT '配置说明',
    `value_type`        VARCHAR(32)     NOT NULL DEFAULT 'string' COMMENT '值类型：string/int/boolean',
    `is_encrypted`      TINYINT         NOT NULL DEFAULT 0 COMMENT '是否加密存储：0否 1是',
    `sort_order`        INT             DEFAULT 0 COMMENT '排序号',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`),
    KEY `idx_group` (`config_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统参数配置表';

-- ============================================================
-- 7. 操作日志表
-- ============================================================
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `biz_type`          VARCHAR(64)     NOT NULL COMMENT '业务类型：order_audit/doc_upload/dlq_retry/config_update',
    `biz_id`            VARCHAR(64)     DEFAULT '' COMMENT '业务ID（如工单号/文档ID）',
    `operator`          VARCHAR(64)     NOT NULL COMMENT '操作人',
    `action`            VARCHAR(128)    NOT NULL COMMENT '操作动作',
    `detail`            TEXT            COMMENT '操作详情（JSON格式）',
    `ip_address`        VARCHAR(64)     DEFAULT '' COMMENT '操作IP地址',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (`id`),
    KEY `idx_biz_type` (`biz_type`),
    KEY `idx_biz_id` (`biz_id`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ============================================================
-- 8. 催单通知记录表
-- ============================================================
DROP TABLE IF EXISTS `reminder_log`;
CREATE TABLE `reminder_log` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `ticket_id`         BIGINT          NOT NULL COMMENT '工单ID',
    `ticket_no`         VARCHAR(32)     NOT NULL COMMENT '工单号',
    `reminder_type`     TINYINT         NOT NULL COMMENT '催单类型：1超时催单',
    `reminder_to`       VARCHAR(64)     NOT NULL COMMENT '被通知人',
    `reminder_channel`  VARCHAR(32)     DEFAULT 'system' COMMENT '通知渠道：system/email/sms',
    `reminder_status`   TINYINT         NOT NULL DEFAULT 0 COMMENT '状态：0已发送 1已处理',
    `sent_at`           DATETIME        NOT NULL COMMENT '发送时间',
    `handled_at`        DATETIME        DEFAULT NULL COMMENT '处理时间',
    `created_at`        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (`id`),
    KEY `idx_ticket_id` (`ticket_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='催单通知记录表';

-- ============================================================
-- 种子数据：系统参数配置
-- ============================================================
INSERT INTO `system_config` (`config_group`, `config_key`, `config_value`, `config_desc`, `value_type`, `sort_order`) VALUES

-- Redis 限流配置
('redis', 'rate_limit.max_requests_per_min', '5',     '单用户每分钟最大售后提交次数',       'int',     1),
('redis', 'rate_limit.token_bucket_capacity', '100',   '令牌桶容量',                         'int',     2),
('redis', 'cache.qa_ttl_seconds',            '300',   '问答缓存过期时间（秒）',              'int',     3),
('redis', 'rate_limit.enabled',              'true',  '限流开启状态',                        'boolean', 4),

-- AI RAG 配置
('ai_rag', 'ollama.base_url',  'http://localhost:11434', 'Ollama大模型服务地址',             'string',  1),
('ai_rag', 'rag.top_n',        '3',    'RAG召回TopN文档数量',                               'int',     2),
('ai_rag', 'chunk.size',       '512',  '文本切片Chunk大小（字符数）',                        'int',     3),

-- MQ 延迟工单配置
('mq', 'timeout.manual_review_hours', '24',  '人工待处理工单超时催单时长（小时）',           'int',     1),
('mq', 'retry.max_attempts',          '3',   '消息重试最大次数',                             'int',     2);

-- ============================================================
-- 种子数据：店铺信息
-- ============================================================
INSERT INTO `store_info` (`store_code`, `store_name`, `category`, `contact_name`, `contact_phone`, `status`) VALUES
('S001', 'XX数码旗舰店',     '数码', '王经理', '13800138001', 1),
('S002', 'XX服饰旗舰店',     '服饰', '李经理', '13800138002', 1),
('S003', 'XX生鲜专营店',     '生鲜', '张经理', '13800138003', 1),
('S004', 'XX家电官方旗舰店', '家电', '赵经理', '13800138004', 1),
('S005', 'XX美妆旗舰店',     '美妆', '陈经理', '13800138005', 1),
('S006', 'XX食品专营店',     '生鲜', '刘经理', '13800138006', 1),
('S007', 'XX母婴旗舰店',     '母婴', '周经理', '13800138007', 1),
('S008', 'XX运动户外店',     '运动', '吴经理', '13800138008', 1),
('S009', 'XX宠物用品店',     '生鲜', '郑经理', '13800138009', 1),
('S010', 'XX图书专营店',     '图书', '孙经理', '13800138010', 1);

-- ============================================================
-- 种子数据：订单信息
-- ============================================================
INSERT INTO `order_info` (`order_no`, `store_id`, `user_id`, `user_name`, `user_phone`, `product_name`, `product_spec`, `pay_amount`, `order_status`, `order_time`) VALUES
('DD998765', 1, 10001, '张三', '13812340001', 'iPhone 15 Pro Max 256GB',  '深黑色 / 256GB',   8999.00, 3, '2026-06-03 14:30:22'),
('DD998766', 2, 10002, '李四', '13812340002', '夏季纯棉T恤男装',         'XXL / 深蓝色',      129.00, 3, '2026-06-04 09:15:00'),
('DD998767', 3, 10003, '王五', '13812340003', '进口车厘子5斤装',         '5斤 / JJ级',        268.00, 3, '2026-06-03 08:00:00'),
('DD998700', 5, 10004, '赵六', '13812340004', '精华液修护套装',           '30ml × 2瓶',        459.00, 3, '2026-06-02 16:30:00'),
('DD998680', 4, 10005, '陈七', '13812340005', '65寸OLED智能电视',        '65英寸 / 黑色',    5999.00, 2, '2026-06-01 11:20:00'),
('DD998590', 7, 10006, '刘八', '13812340006', '婴儿纸尿裤XL码',          'XL码 / 60片装',     189.00, 3, '2026-06-03 08:55:00'),
('DD998450', 8, 10007, '周九', '13812340007', '专业跑步鞋',               '42码 / 白色',       699.00, 3, '2026-06-02 20:05:00');

-- ============================================================
-- 种子数据：售后工单
-- ============================================================
INSERT INTO `after_sale_order` (`ticket_no`, `order_id`, `order_no`, `store_id`, `user_id`,
    `after_sale_type`, `apply_reason`, `evidence_images`, `apply_amount`,
    `ai_audit_result`, `ai_confidence`, `ai_audit_status`, `ai_audit_time`,
    `ticket_status`, `rag_rule_ids`, `created_at`) VALUES
('SH20260605001', 1, 'DD998765', 1, 10001,
 1, '商品屏幕碎裂，无法正常使用', '["https://oss.example.com/img01.jpg"]', 8999.00,
 '同意全额退款', 94.30, 1, '2026-06-05 10:20:15',
 1, '12,8,5', '2026-06-05 10:20:00'),

('SH20260605002', 2, 'DD998766', 2, 10002,
 2, '尺码不合适，买大了需要换货', '', 129.00,
 '待人工判定', 72.50, 2, '2026-06-05 11:12:30',
 2, '', '2026-06-05 11:12:00'),

('SH20260605003', 3, 'DD998767', 3, 10003,
 3, '商品变质发臭，有异味', '["https://oss.example.com/img03.jpg","https://oss.example.com/img04.jpg"]', 268.00,
 '建议全额退款', 91.80, 1, '2026-06-05 09:45:10',
 1, '15,22,3', '2026-06-05 09:45:00'),

('SH20260604015', 5, 'DD998700', 5, 10004,
 1, '未收到货，物流显示已签收但本人未取', '', 459.00,
 '证据不足，建议驳回', 68.40, 2, '2026-06-04 16:30:00',
 2, '7,18', '2026-06-04 16:30:00'),

('SH20260604008', 4, 'DD998680', 4, 10005,
 2, '电视开机有坏点，功能故障', '["https://oss.example.com/img05.jpg"]', 5999.00,
 '待人工判定', 65.30, 2, '2026-06-04 14:10:00',
 2, '', '2026-06-04 14:10:00'),

('SH20260603022', 7, 'DD998590', 7, 10006,
 1, '商品与描述不符，纸尿裤尺寸偏小', '["https://oss.example.com/img06.jpg"]', 189.00,
 '同意退款', 88.60, 1, '2026-06-03 08:55:20',
 1, '10,2', '2026-06-03 08:55:00'),

('SH20260602005', 8, 'DD998450', 8, 10007,
 3, '客服态度恶劣，拒绝处理售后', '', 699.00,
 '转人工处理', 45.20, 2, '2026-06-02 20:05:00',
 4, '', '2026-06-02 20:05:00');

-- ============================================================
-- 种子数据：AI审核日志
-- ============================================================
INSERT INTO `ai_audit_log` (`ticket_id`, `ticket_no`, `llm_model`, `llm_prompt`, `llm_response`,
    `llm_latency_ms`, `rag_chunk_ids`, `rag_similarities`, `audit_conclusion`, `confidence`, `suggested_action`) VALUES
(1, 'SH20260605001', 'qwen2.5:7b',
 '系统角色：你是电商售后审核专家...参考规则：[#1 平台规则第3章第2条 相似度96.8%]...',
 '{"conclusion":"同意全额退款","confidence":94.3,"reason":"符合7天无理由退货政策","suggestedAction":"全额退款"}',
 2846, '12,8,5', '96.8,91.2,85.7', '同意全额退款', 94.30, '全额退款'),

(2, 'SH20260605002', 'qwen2.5:7b',
 '系统角色：你是电商售后审核专家...用户申请：尺码不合适...',
 '{"conclusion":"待人工判定","confidence":72.5,"reason":"尺码问题需人工确认商品状态","suggestedAction":"建议人工核实"}',
 3102, '', '', '待人工判定', 72.50, '建议人工核实'),

(3, 'SH20260605003', 'qwen2.5:7b',
 '系统角色：你是电商售后审核专家...生鲜变质投诉...',
 '{"conclusion":"建议全额退款","confidence":91.8,"reason":"生鲜变质属商家责任","suggestedAction":"全额退款并补偿"}',
 1953, '15,22,3', '93.5,87.2,80.1', '建议全额退款', 91.80, '全额退款并补偿');

-- ============================================================
-- 种子数据：MQ死信消息
-- ============================================================
INSERT INTO `dead_letter_message` (`msg_id`, `queue_name`, `exchange_name`, `routing_key`,
    `ticket_no`, `error_reason`, `msg_content`, `retry_count`, `max_retry`, `dlq_status`, `error_time`) VALUES
('MSG10001', 'queue.refund.only',   'exchange.after.sale', 'refund.only.created',
 'SH20260605001', '大模型Ollama调用超时（30s无响应）',
 '{"msgId":"MSG10001","ticketNo":"SH20260605001","afterSaleType":1}', 3, 3, 0,
 '2026-06-05 10:20:15'),

('MSG10002', 'queue.refund.return', 'exchange.after.sale', 'refund.return.created',
 'SH20260605002', '文档解析异常：PDF格式不被支持',
 '{"msgId":"MSG10002","ticketNo":"SH20260605002","afterSaleType":2}', 3, 3, 0,
 '2026-06-05 09:45:33'),

('MSG10003', 'queue.doc.parse',     'exchange.knowledge',  'doc.parse',
 'SH20260604015', '向量化超时：PgVector连接池耗尽',
 '{"msgId":"MSG10003","docId":5,"filePath":"/data/docs/售后规则.pdf"}', 2, 3, 0,
 '2026-06-04 18:22:01'),

('MSG10004', 'queue.complaint',     'exchange.after.sale', 'complaint.created',
 'SH20260604008', '参数异常：用户ID为空',
 '{"msgId":"MSG10004","ticketNo":"SH20260604008","userId":null}', 3, 3, 0,
 '2026-06-04 16:10:44'),

('MSG10005', 'queue.refund.only',   'exchange.after.sale', 'refund.only.created',
 'SH20260603022', 'Redis分布式锁获取失败（锁已被占用）',
 '{"msgId":"MSG10005","ticketNo":"SH20260603022"}', 1, 3, 1,
 '2026-06-03 22:35:12'),

('MSG10006', 'queue.doc.parse',     'exchange.knowledge',  'doc.parse',
 '', 'MQ消息体序列化异常：JSON格式错误',
 '{"msgId":"MSG10006",invalid}', 3, 3, 0,
 '2026-06-03 14:05:28');

-- ============================================================
-- 种子数据：操作日志
-- ============================================================
INSERT INTO `operation_log` (`biz_type`, `biz_id`, `operator`, `action`, `detail`, `ip_address`, `created_at`) VALUES
('order_audit', 'SH20260605001', 'system', 'AI审核完成 - 同意全额退款',
 '{"aiConclusion":"同意全额退款","confidence":94.3}', '192.168.1.10', '2026-06-05 10:20:16'),
('order_audit', 'SH20260605002', 'system', 'AI审核完成 - 待人工判定',
 '{"aiConclusion":"待人工判定","confidence":72.5}', '192.168.1.10', '2026-06-05 11:12:31'),
('doc_upload', 'DOC001', '管理员', '上传知识库文档',
 '{"docName":"平台7天无理由售后规则.pdf","category":"platform_general"}', '192.168.1.100', '2026-05-20 10:00:00'),
('dlq_retry', 'MSG10005', '管理员', '死信消息重试',
 '{"msgId":"MSG10005","targetQueue":"queue.refund.only"}', '192.168.1.100', '2026-06-04 09:00:00'),
('config_update', '1', '管理员', '修改限流配置',
 '{"configKey":"rate_limit.max_requests_per_min","oldValue":"5","newValue":"10"}', '192.168.1.100', '2026-06-03 14:00:00');

-- ============================================================
-- 种子数据：催单通知
-- ============================================================
INSERT INTO `reminder_log` (`ticket_id`, `ticket_no`, `reminder_type`, `reminder_to`, `reminder_channel`, `reminder_status`, `sent_at`) VALUES
(2, 'SH20260605002', 1, '客服组', 'system', 0, '2026-06-06 11:12:00');

-- ============================================================
-- 收尾：校验数据
-- ============================================================
SELECT '初始化完成' AS status,
       (SELECT COUNT(*) FROM store_info) AS store_count,
       (SELECT COUNT(*) FROM order_info) AS order_count,
       (SELECT COUNT(*) FROM after_sale_order) AS ticket_count,
       (SELECT COUNT(*) FROM system_config) AS config_count;
