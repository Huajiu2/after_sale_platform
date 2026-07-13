-- ============================================================
-- 电商智能售后中台 · PostgreSQL + pgvector 初始化脚本
-- 包含：扩展安装、建表、索引、种子数据
-- 适用版本：PostgreSQL 14+ / pgvector 0.5+
-- ============================================================

-- 启用 pgvector 扩展
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================================
-- 1. 知识库文档表
-- ============================================================
DROP TABLE IF EXISTS rag_document CASCADE;
CREATE TABLE rag_document (
    id                  BIGSERIAL       PRIMARY KEY,
    doc_code            VARCHAR(32)     NOT NULL UNIQUE,
    doc_name            VARCHAR(256)     NOT NULL,
    doc_category        VARCHAR(32)     NOT NULL,
    file_type           VARCHAR(16)     NOT NULL,
    file_size           INT             NOT NULL,
    file_path           VARCHAR(512)    NOT NULL,
    original_name       VARCHAR(256)    NOT NULL,
    chunk_count         INT             DEFAULT 0,
    vectorize_status    SMALLINT        NOT NULL DEFAULT 0,
    vectorize_error     VARCHAR(512)    DEFAULT '',
    uploaded_by         VARCHAR(64)     DEFAULT '',
    uploaded_at         TIMESTAMP       NOT NULL DEFAULT NOW(),
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  rag_document       IS '知识库文档表';
COMMENT ON COLUMN rag_document.doc_code IS '文档编号，如 DOC001';
COMMENT ON COLUMN rag_document.doc_name IS '文档名称';
COMMENT ON COLUMN rag_document.doc_category IS '分类：platform_general / digital / fresh / history_case';
COMMENT ON COLUMN rag_document.file_type IS '文件类型：pdf / docx / txt';
COMMENT ON COLUMN rag_document.file_size IS '文件大小（字节）';
COMMENT ON COLUMN rag_document.file_path IS '存储路径';
COMMENT ON COLUMN rag_document.original_name IS '原始文件名';
COMMENT ON COLUMN rag_document.chunk_count IS '切片数量';
COMMENT ON COLUMN rag_document.vectorize_status IS '状态：0待解析 1解析中 2已向量化 3失败';
COMMENT ON COLUMN rag_document.vectorize_error IS '向量化失败原因';
COMMENT ON COLUMN rag_document.uploaded_by IS '上传人';
COMMENT ON COLUMN rag_document.uploaded_at IS '上传时间';

CREATE INDEX idx_doc_category     ON rag_document(doc_category);
CREATE INDEX idx_vectorize_status ON rag_document(vectorize_status);

-- ============================================================
-- 2. 文档切片表（向量检索核心表）
-- ============================================================
DROP TABLE IF EXISTS rag_chunk CASCADE;
CREATE TABLE rag_chunk (
    id                  BIGSERIAL       PRIMARY KEY,
    doc_id              BIGINT          NOT NULL REFERENCES rag_document(id) ON DELETE CASCADE,
    chunk_index         INT             NOT NULL,
    chunk_text          TEXT            NOT NULL,
    embedding           VECTOR(1536),
    token_count         INT             DEFAULT 0,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  rag_chunk       IS '文档切片表（向量检索核心）';
COMMENT ON COLUMN rag_chunk.doc_id IS '所属文档ID';
COMMENT ON COLUMN rag_chunk.chunk_index IS '切片序号，从0开始';
COMMENT ON COLUMN rag_chunk.chunk_text IS '切片文本内容';
COMMENT ON COLUMN rag_chunk.embedding IS '1536维文本向量嵌入';
COMMENT ON COLUMN rag_chunk.token_count IS '切片token数';

-- ivfflat 索引
CREATE INDEX idx_rag_chunk_embedding ON rag_chunk
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

CREATE INDEX idx_chunk_doc_id ON rag_chunk(doc_id);

-- ============================================================
-- 3. 优质判例表（自迭代判例库）
-- ============================================================
DROP TABLE IF EXISTS quality_case CASCADE;
CREATE TABLE quality_case (
    id                  BIGSERIAL       PRIMARY KEY,
    ticket_no           VARCHAR(32)     NOT NULL UNIQUE,
    case_category       VARCHAR(32)     NOT NULL,
    case_title          VARCHAR(256)    NOT NULL,
    case_content        TEXT            NOT NULL,
    final_result        VARCHAR(256)    NOT NULL,
    manual_remark       VARCHAR(1024)   DEFAULT '',
    embedding           VECTOR(1536),
    is_active           BOOLEAN         DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE quality_case IS '优质判例表（自迭代）';
COMMENT ON COLUMN quality_case.ticket_no IS '来源工单号';
COMMENT ON COLUMN quality_case.case_category IS '判例分类：refund_only / return / complaint';
COMMENT ON COLUMN quality_case.case_title IS '判例标题';
COMMENT ON COLUMN quality_case.case_content IS '判例正文（用于向量化检索）';
COMMENT ON COLUMN quality_case.final_result IS '最终处理结果';
COMMENT ON COLUMN quality_case.manual_remark IS '人工备注';
COMMENT ON COLUMN quality_case.embedding IS '判例向量';
COMMENT ON COLUMN quality_case.is_active IS '是否生效';

CREATE INDEX idx_quality_case_embedding ON quality_case
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 50);

CREATE INDEX idx_case_category ON quality_case(case_category);

-- ============================================================
-- 工具函数：向量相似度检索
-- ============================================================
CREATE OR REPLACE FUNCTION search_similar_chunks(
    query_embedding VECTOR(1536),
    top_n INT DEFAULT 3
)
RETURNS TABLE(
    chunk_id        BIGINT,
    chunk_text      TEXT,
    doc_id          BIGINT,
    doc_name        VARCHAR(256),
    doc_category    VARCHAR(32),
    similarity      NUMERIC
)
LANGUAGE SQL STABLE
AS $$
    SELECT
        c.id            AS chunk_id,
        c.chunk_text,
        c.doc_id,
        d.doc_name,
        d.doc_category,
        ROUND((1 - (c.embedding <=> query_embedding))::NUMERIC * 100, 1) AS similarity
    FROM rag_chunk c
    JOIN rag_document d ON d.id = c.doc_id
    WHERE d.vectorize_status = 2
    ORDER BY c.embedding <=> query_embedding
    LIMIT top_n;
$$;

CREATE OR REPLACE FUNCTION search_combined_knowledge(
    query_embedding VECTOR(1536),
    top_n INT DEFAULT 3
)
RETURNS TABLE(
    content     TEXT,
    source      VARCHAR(256),
    category    VARCHAR(32),
    similarity  NUMERIC
)
LANGUAGE SQL STABLE
AS $$
    SELECT * FROM (
        SELECT
            c.chunk_text            AS content,
            d.doc_name              AS source,
            d.doc_category          AS category,
            ROUND((1 - (c.embedding <=> query_embedding))::NUMERIC * 100, 1) AS similarity
        FROM rag_chunk c
        JOIN rag_document d ON d.id = c.doc_id
        WHERE d.vectorize_status = 2

        UNION ALL

        SELECT
            q.case_content          AS content,
            CONCAT('【判例】', q.case_title) AS source,
            q.case_category         AS category,
            ROUND((1 - (q.embedding <=> query_embedding))::NUMERIC * 100, 1) AS similarity
        FROM quality_case q
        WHERE q.is_active = TRUE
    ) AS combined
    ORDER BY similarity DESC
    LIMIT top_n;
$$;

-- ============================================================
-- 种子数据
-- ============================================================
INSERT INTO rag_document (doc_code, doc_name, doc_category, file_type, file_size, file_path, original_name,
                          chunk_count, vectorize_status, uploaded_by, uploaded_at)
VALUES
('DOC001', '平台7天无理由售后规则.pdf', 'platform_general', 'pdf', 1258291,
 '/data/knowledge/DOC001_platform_7d_return.pdf', '平台7天无理由售后规则.pdf',
 28, 2, '管理员', '2026-05-20 10:00:00'),

('DOC002', '数码3C售后处理规范.docx',   'digital',          'docx', 876544,
 '/data/knowledge/DOC002_digital_after_sale.docx', '数码3C售后处理规范.docx',
 16, 2, '管理员', '2026-05-18 14:30:00'),

('DOC003', '生鲜食品售后标准流程.pdf',   'fresh',            'pdf',  2202009,
 '/data/knowledge/DOC003_fresh_standard.pdf', '生鲜食品售后标准流程.pdf',
 42, 2, '管理员', '2026-05-15 09:00:00'),

('DOC004', '2026年5月典型判例汇总.txt',  'history_case',     'txt',  331776,
 '/data/knowledge/DOC004_case_may2026.txt', '2026年5月典型判例汇总.txt',
 9, 1, '管理员', '2026-06-04 16:00:00'),

('DOC005', '服装鞋帽售后规则手册.pdf',   'platform_general', 'pdf',  4718592,
 '/data/knowledge/DOC005_apparel_rules.pdf', '服装鞋帽售后规则手册.pdf',
 85, 2, '管理员', '2026-05-10 11:00:00');

INSERT INTO rag_chunk (doc_id, chunk_index, chunk_text, token_count) VALUES
(1, 0, '第一章 总则 1.1 为保障消费者权益，明确平台售后服务标准，特制定本规则。1.2 本规则适用于平台内所有入驻商家及消费者，自发布之日起生效。', 65),
(1, 1, '1.3 平台有权根据法律法规及业务发展需要对本规则进行修订，修订后的规则将提前7天公示。', 42),
(1, 4, '第3章 售后处理流程 3.1 消费者在签收商品之日起7日内，如对商品不满意，在不影响二次销售的前提下，可向卖家发起7天无理由退货申请。', 68),
(1, 5, '3.2 消费者发起的售后申请，平台AI系统将在30秒内完成审核。审核通过的，卖家应在48小时内完成退款或换货处理。', 60),
(1, 11, '3.5 对于生鲜类商品，不适用7天无理由退货，但如出现商品变质、腐烂等质量问题，消费者可在签收后24小时内提供照片凭证申请售后。', 72),
(1, 19, '第6章 违规处理 6.1 卖家拒绝履行售后义务的，平台有权按照《平台商家违规管理规则》对卖家进行扣分、降权等处理。', 59),

(2, 0, '数码3C售后处理规范 第1条 适用范围：本规范适用于数码产品（手机、电脑、相机、智能穿戴设备等）的售后处理。', 57),
(2, 4, '第5条 对于屏幕碎裂类投诉：若买家上传照片显示碎屏区域存在明显外力撞击点，需转人工判定；若照片显示无外力痕迹的内裂，可直接同意售后。', 72),
(2, 8, '第9条 性能故障类：对于开机黑屏、无法充电、主板烧毁等性能故障，优先引导用户至官方授权维修点检测，凭检测报告处理。', 65),

(3, 0, '生鲜食品售后标准流程 一、适用范围 本流程适用于生鲜水果、肉禽蛋奶、冷冻食品等品类。', 43),
(3, 3, '二、变质处理标准 2.1 消费者签收后24小时内反馈商品变质、发臭的，提供清晰照片后可直接办理全额退款。', 56),
(3, 7, '三、少发/漏发处理 3.1 消费者反馈少发漏发的，需提供称重照片或开箱视频作为凭证。', 43),

(5, 0, '服装鞋帽售后规则手册 第一章 退换货政策 1.1 服装类商品支持7天无理由退换货，但需保证吊牌完整、未水洗、无异味。', 64),
(5, 2, '1.3 鞋类商品：试穿时请在地毯等柔软表面进行，鞋底有磨损痕迹的不予退换。', 42),
(5, 10, '第三章 质量问题的判定标准 3.1 开线/脱缝：缝合处开裂长度超过2cm的属于质量问题。', 49);

INSERT INTO quality_case (ticket_no, case_category, case_title, case_content, final_result, manual_remark, is_active)
VALUES
('SH20260512001', 'refund_only',
 '手机碎屏-同意退款',
 '用户购买手机后第三天发现屏幕出现裂纹，申请仅退款。用户上传照片显示裂纹从屏幕左下角延伸，无外力撞击点。引用规则：平台7天无理由售后规则第3章第2条；数码3C售后规范第5条。人工审核确认：同意全额退款。',
 '同意全额退款', '照片显示内裂无撞击点，符合规则，已退款处理。', TRUE),

('SH20260515003', 'refund_return',
 '服装尺码不合适-退货退款',
 '用户购买运动鞋尺码偏大，申请退货退款。商品未经穿着，鞋底无磨损，包装完好。引用规则：服装鞋帽售后规则手册第1章第1条。人工审核确认：同意退货退款。',
 '同意退货退款', '商品完好不影响二次销售，符合7天无理由退货政策。', TRUE),

('SH20260520007', 'complaint',
 '生鲜变质-全额退款并补偿',
 '用户购买进口车厘子，签收后6小时内反馈果实发霉变质。上传照片清晰可见霉斑。引用规则：生鲜食品售后标准流程第二条；平台规则第3.5条。人工审核确认：全额退款并给予20元优惠券补偿。',
 '全额退款并补偿优惠券', '生鲜变质属实，已退款并补偿安抚用户。', TRUE),

('SH20260522002', 'refund_only',
 '未收到货-驳回申请',
 '用户申请仅退款声称未收到货，但物流系统显示已由本人签收（签收底单与用户姓名一致）。引用规则：平台规则第4.2条。人工审核确认：驳回申请。',
 '驳回申请', '物流底单显示本人签收，驳回用户售后申请。', TRUE);

-- 校验数据
SELECT 'PgVector初始化完成' AS status,
       (SELECT COUNT(*) FROM rag_document) AS doc_count,
       (SELECT COUNT(*) FROM rag_chunk)    AS chunk_count,
       (SELECT COUNT(*) FROM quality_case) AS case_count;