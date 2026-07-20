package com.example.aftersight.service.impl;

import com.example.aftersight.common.AiAuditResult;
import com.example.aftersight.common.Result;
import com.example.aftersight.dto.BatchAssignDTO;
import com.example.aftersight.dto.ManualAuditDTO;
import com.example.aftersight.dto.SubmitDTO;
import com.example.aftersight.entity.AfterSaleOrder;
import com.example.aftersight.entity.AiAuditLog;
import com.example.aftersight.entity.OperationLog;
import com.example.aftersight.entity.OrderInfo;
import com.example.aftersight.enums.AfterSaleTypeEnum;
import com.example.aftersight.enums.TicketStatusEnum;
import com.example.aftersight.mapper.AfterSaleMapper;
import com.example.aftersight.mq.AuditMessageDTO;
import com.example.aftersight.mq.MqProducer;
import com.example.aftersight.service.AfterSaleService;
import com.example.aftersight.utils.ImageUploadUtils;
import com.example.aftersight.vo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Service
public class AfterSaleServiceImpl implements AfterSaleService {

    @Resource
    private AfterSaleMapper afterSaleMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Autowired
    private ImageUploadUtils uploadUtils;

    @Autowired
    private ContentRetriever contentRetriever;

    @Autowired
    private MqProducer mqProducer;

    @Resource
    private ObjectMapper objectMapper;

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    // 前缀常量
    private static final String PREFIX = "SH";
    // Redis key 前缀
    private static final String REDIS_KEY_PREFIX = "ticket:seq:";
    // 日期格式化 yyyyMMdd
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String RATE_LIMIT_KEY_PREFIX = "rate:limit:token_bucket:";
    private static final String TOKEN_BUCKET_CAPACITY = "5";
    private static final String TOKEN_REFILL_INTERVAL_MILLIS = "60000";
    private static final DefaultRedisScript<Long> acquireToken;

    static {
        acquireToken = new DefaultRedisScript<>();
        acquireToken.setLocation(new ClassPathResource("token.lua"));
        acquireToken.setResultType(Long.class);
    }

    /**
     * 用户提交售后申请
     *
     * @param submitDTO
     * @return
     */
    @Override
    @Transactional
    public Result<SubmitVO> submit(SubmitDTO submitDTO) {
        SubmitVO submitVO = new SubmitVO();
        List<String> imageUrl = new ArrayList<>();

        //订单号为空
        String orderNo = submitDTO.getOrderNo();
        if (orderNo == null || orderNo.isBlank()) {
            return Result.fail(400, "订单号不存在");
        }
        List<String> nos = afterSaleMapper.selectAllOrderNos();
        //布隆过滤器过滤不存在的订单
        RBloomFilter<Object> bloom = redissonClient.getBloomFilter("bloom:order_no");
        bloom.tryInit(1000000, 0.03);
        //todo 只执行一次，后面会注释掉这一行
        //addOrderNos(bloom, nos);
        boolean contains = bloom.contains(orderNo);
        if (!contains) {
            return Result.fail(400, "订单号不存在");
        }

        //查MySQL订单
        OrderInfo order = afterSaleMapper.getOrder(orderNo);
        //订单不存在，返回失败
        if (order == null) {
            return Result.fail(400, "订单号不存在");
        }

        List<String> keys = Collections.singletonList(RATE_LIMIT_KEY_PREFIX + order.getUserId());
        //redis令牌桶限流
        //令牌桶一次请求里要做很多动作,因此使用Lua脚本确保操作的原子性
        Long execute = stringRedisTemplate.execute(
                acquireToken,
                keys,
                TOKEN_BUCKET_CAPACITY,//桶容量
                TOKEN_REFILL_INTERVAL_MILLIS,//60000，每 60 秒补充 1 个令牌
                String.valueOf(System.currentTimeMillis())//当前时间戳
        );

        if (execute == null || execute != 1L) {
            return Result.fail(429, "提交过于频繁，请稍后再试");
        }

        //遍历用户上传的图片并将其上传到阿里云OSS中
        MultipartFile[] files = submitDTO.getEvidenceFiles();
        if (files != null) {
            for (MultipartFile evidenceFile : files) {
                if (evidenceFile.isEmpty()) continue;
                String image = uploadUtils.uploadImage(evidenceFile, evidenceFile.getOriginalFilename());
                if (org.springframework.util.StringUtils.hasText(image)) {
                    imageUrl.add(image);
                }
            }
        }

        String date = LocalDateTime.now().format(DATE_FORMATTER);
        String key = REDIS_KEY_PREFIX + date;
        //生成工单号
        Long seq = stringRedisTemplate.opsForValue().increment(key);
        String ticketNo = PREFIX + date + String.format("%03d", seq);
        submitVO.setTicketNo(ticketNo);

        //组装售后工单表
        //将售后记录插入数据库
        AfterSaleOrder afterSaleOrder = new AfterSaleOrder();
        afterSaleOrder.setTicketNo(ticketNo);
        afterSaleOrder.setOrderId(order.getId());
        afterSaleOrder.setOrderNo(orderNo);
        afterSaleOrder.setStoreId(order.getStoreId());
        afterSaleOrder.setUserId(order.getUserId());
        afterSaleOrder.setAfterSaleType(submitDTO.getAfterSaleType());
        afterSaleOrder.setApplyReason(submitDTO.getApplyReason());
        afterSaleOrder.setEvidenceImages(new Gson().toJson(imageUrl));
        afterSaleOrder.setApplyAmount(order.getPayAmount());
        afterSaleOrder.setAiAuditStatus(0);
        afterSaleOrder.setTicketStatus(0);
        afterSaleOrder.setRetryCount(0);

        afterSaleMapper.addAfterSaleOrder(afterSaleOrder);


        //投递MQ消息
        AuditMessageDTO msg = new AuditMessageDTO();
        BeanUtils.copyProperties(afterSaleOrder, msg);
        Long msgSeq = stringRedisTemplate.opsForValue().increment("msg:seq:" + date);
        msg.setMsgId("MSG" + date + String.format("%04d", msgSeq));
        msg.setTimestamp(LocalDateTime.now());

        mqProducer.sendAuditMessage(msg);


        submitVO.setTicketStatus(0);
        submitVO.setEstimatedTime("30秒内出结果");
        submitVO.setMessage("售后申请提交成功！");
        return Result.success(submitVO);
    }


    //将所有订单号添加到布隆过滤器中
    public void addOrderNos(RBloomFilter<Object> bloom, List<String> nos) {
        for (String no : nos) {
            bloom.add(no);
        }
    }


    /**
     * 工单列表分页查询实现类
     */
    @Override
    public List<AfterSaleOrderListVO> getAfterSaleOrder() {
        return afterSaleMapper.getAfterSaleOrder();
    }


    /**
     * 工单列表详情查询实现类
     *
     * @param ticketNo
     * @return
     */
    @Transactional
    @Override
    public Result<AfterSaleDetailVO> getAfterSaleOrderDetail(String ticketNo) {
        //ticketNo,orderInfo{},afterSaleInfo{}
        AfterSaleDetailVO afterSaleDetailVO = new AfterSaleDetailVO();
        afterSaleDetailVO.setTicketNo(ticketNo);

        AfterSaleOrder afterSaleOrder = afterSaleMapper.getByTicketNo(ticketNo);
        if (afterSaleOrder == null) {
            return Result.fail(400, "工单不存在");
        }
        //orderInfo{}  订单信息
        String orderNo = afterSaleMapper.getOrderNo(ticketNo);
        OrderInfoVO orderInfoVO = afterSaleMapper.getOrderVO(orderNo);
        afterSaleDetailVO.setOrderInfo(orderInfoVO);

        //afterSaleInfo{}  售后工单信息
        AfterSaleInfoVO afterSaleInfoVO = new AfterSaleInfoVO();
        afterSaleInfoVO.setAfterSaleType(afterSaleOrder.getAfterSaleType());
        AfterSaleTypeEnum typeEnum = AfterSaleTypeEnum.fromCode(afterSaleOrder.getAfterSaleType());
        afterSaleInfoVO.setAfterSaleTypeDesc(typeEnum != null ? typeEnum.getDesc() : "");
        afterSaleInfoVO.setApplyReason(afterSaleOrder.getApplyReason());
        // evidenceImages 数据库存的是 JSON 字符串，转为 List
        String evidenceJson = afterSaleOrder.getEvidenceImages();
        if (StringUtils.hasText(evidenceJson)) {
            afterSaleInfoVO.setEvidenceImages(new Gson().fromJson(evidenceJson, List.class));
        } else {
            afterSaleInfoVO.setEvidenceImages(new ArrayList<>());
        }
        afterSaleInfoVO.setApplyAmount(afterSaleOrder.getApplyAmount());
        afterSaleDetailVO.setAfterSaleInfo(afterSaleInfoVO);

        //ragEvidence   RAG证据
        //ragEvidence  RAG检索依据
//        List<Content> contents = contentRetriever.retrieve(Query.from(afterSaleOrder.getApplyReason()));
//        List<RagEvidenceVO> ragEvidenceList = new ArrayList<>();
//        int rank = 1;
//        for (Content content : contents) {
//            RagEvidenceVO ev = new RagEvidenceVO();
//            ev.setRank(rank++);
//            ev.setRuleContent(content.textSegment().text());
//            ev.setSourceDoc(content.textSegment().metadata().getString("file_name"));
//            ragEvidenceList.add(ev);
//        }
//        afterSaleDetailVO.setRagEvidence(ragEvidenceList);
        //ragEvidence  RAG检索依据（含相似度分数）
        List<RagEvidenceVO> ragEvidenceList = new ArrayList<>();
        if (StringUtils.hasText(afterSaleOrder.getApplyReason())) {
            Embedding queryEmbedding = embeddingModel.embed(afterSaleOrder.getApplyReason()).content();
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(5)
                    .minScore(0.75)
                    .build();
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(searchRequest).matches();
            int rank = 1;
            for (EmbeddingMatch<TextSegment> match : matches) {
                RagEvidenceVO ev = new RagEvidenceVO();
                ev.setRank(rank++);
                ev.setSimilarity(BigDecimal.valueOf(match.score() * 100).setScale(2, java.math.RoundingMode.HALF_UP));
                ev.setRuleContent(match.embedded().text());
                ev.setSourceDoc(match.embedded().metadata().getString("file_name"));
                ragEvidenceList.add(ev);
            }
        }
        afterSaleDetailVO.setRagEvidence(ragEvidenceList);


        //AI审核详情
        AiAuditLog auditLog = afterSaleMapper.getAiAuditLog(ticketNo);
        if (auditLog != null) {
            AiAuditDetailVO detailVO = new AiAuditDetailVO();
            detailVO.setConclusion(auditLog.getAuditConclusion());
            detailVO.setConfidence(auditLog.getConfidence());
            detailVO.setSuggestedAction(auditLog.getSuggestedAction());
            detailVO.setModelName("gpt-5.5");
            detailVO.setLatencyMs(auditLog.getLlmLatencyMs());
            detailVO.setAuditTime(auditLog.getCreatedAt());
            // reason 从 llmResponse 的 JSON 里解析
            if (StringUtils.hasText(auditLog.getLlmResponse())) {
                try {
                    AiAuditResult result = objectMapper.readValue(auditLog.getLlmResponse(), AiAuditResult.class);
                    detailVO.setReason(result.getReason());
                } catch (Exception e) {
                    log.warn("解析AI审核结果JSON失败", e);
                }
            }
            afterSaleDetailVO.setAiAuditDetail(detailVO);
        }

        afterSaleDetailVO.setTicketStatus(afterSaleOrder.getTicketStatus());
        TicketStatusEnum ticketEnum = TicketStatusEnum.fromCode(afterSaleOrder.getTicketStatus());
        afterSaleDetailVO.setTicketStatusDesc(ticketEnum != null ? ticketEnum.getDesc() : "");
        afterSaleDetailVO.setCreatedAt(afterSaleOrder.getCreatedAt());

        return Result.success(afterSaleDetailVO);
    }


    @Transactional
    @Override
    public Result manualAuditSubmit(ManualAuditDTO auditDTO) {
        ManualAuditResultVO auditResultVO = new ManualAuditResultVO();

        AfterSaleOrder afterSaleOrder = afterSaleMapper.getByTicketNo(auditDTO.getTicketNo());
        if (afterSaleOrder == null || afterSaleOrder.getTicketStatus() != 2) {
            return Result.fail(402, "工单已办结/已驳回！");
        }
        Integer targetStatus;
        //更新工单状态
        if (auditDTO.getManualResult() == 1) {
            targetStatus = 1;
            auditResultVO.setTicketStatusDesc(TicketStatusEnum.AI_CLOSED.getDesc());
        } else {
            targetStatus = 3;
            auditResultVO.setTicketStatusDesc(TicketStatusEnum.REJECTED.getDesc());
        }
        afterSaleOrder.setTicketStatus(targetStatus);
        afterSaleOrder.setAiAuditStatus(1);
        afterSaleOrder.setManualAuditBy("管理员");  // 可以从登录上下文获取
        afterSaleOrder.setManualRemark(auditDTO.getManualRemark());
        afterSaleOrder.setManualResult(auditDTO.getManualResult());
        int rows = afterSaleMapper.updateManualAudit(afterSaleOrder);
        if (rows == 0) {
            return Result.fail(500, "更新工单失败");
        }

        //记录操作日志
        OperationLog log = new OperationLog();
        log.setBizType("order_audit");
        log.setBizId(auditDTO.getTicketNo());
        log.setOperator("管理员");
        log.setAction("人工审核 - " + (targetStatus == 1 ? "同意售后" : "驳回售后"));
        log.setDetail(new Gson().toJson(auditDTO));
        log.setIpAddress(getClientIp());
        afterSaleMapper.insertOperationLog(log);

        //更新redis仪表盘缓存（删除旧缓存，下次请求自动重建）
        stringRedisTemplate.delete("cache:dashboard:stats");
        String monthKey = "rank:store:monthly:" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        stringRedisTemplate.delete(monthKey);

        auditResultVO.setTicketStatus(targetStatus);
        auditResultVO.setCaseGenerated(StringUtils.hasText(auditDTO.getManualRemark()));
        auditResultVO.setTicketNo(auditDTO.getTicketNo());
        return Result.success(auditResultVO);
    }


    @Transactional
    @Override
    public Result batchAssign(BatchAssignDTO batchAssignDTO) {
        if (batchAssignDTO.getTicketNos() == null || batchAssignDTO.getTicketNos().isEmpty()) {
            return Result.fail(400, "工单号列表不能为空");
        }
        if (!StringUtils.hasText(batchAssignDTO.getAssignee())) {
            return Result.fail(400, "指派人不能为空");
        }

        int success = 0;
        List<String> failList = new ArrayList<>();

        for (String ticketNo : batchAssignDTO.getTicketNos()) {
            int rows = afterSaleMapper.assignByTicketNo(ticketNo, batchAssignDTO.getAssignee());
            if (rows > 0) {
                success++;
            } else {
                failList.add(ticketNo);
            }
        }

        // 记录操作日志
        if (success > 0) {
            OperationLog log = new OperationLog();
            log.setBizType("order_assign");
            log.setBizId(String.join(",", batchAssignDTO.getTicketNos()));
            log.setOperator(batchAssignDTO.getAssignee());
            log.setAction("批量指派");
            log.setDetail(new Gson().toJson(batchAssignDTO));
            log.setIpAddress(getClientIp());
            afterSaleMapper.insertOperationLog(log);
        }

        BatchAssignVO vo = new BatchAssignVO();
        vo.setSuccessCount(success);
        vo.setFailCount(failList.size());

        if (failList.isEmpty()) {
            return Result.success("成功指派 " + success + " 条工单给 " + batchAssignDTO.getAssignee(), vo);
        } else {
            log.info("成功指派 " + success + " 条，失败 " + failList.size() + " 条（可能已指派或状态非待人工）");
            return Result.fail(402,"成功指派 " + success + " 条，失败 " + failList.size() + " 条（可能已指派或状态非待人工）");
        }
    }


    @Transactional
    @Override
    public Result batchRetry(BatchAssignDTO dto) {
        if (dto.getTicketNos()==null)
            return Result.fail(400,"工单列表不能为空");
        int success=0;
        for (String ticketNo:dto.getTicketNos()){
            AfterSaleOrder saleOrder = afterSaleMapper.getByTicketNo(ticketNo);
            if (saleOrder.getTicketStatus()!=0)
                continue;
            AuditMessageDTO msg = new AuditMessageDTO();
            BeanUtils.copyProperties(saleOrder,msg);
            // 生成新消息ID
            String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            Long seq = stringRedisTemplate.opsForValue().increment("msg:seq:retry:" + date);
            msg.setMsgId("RETRY" + date + String.format("%04d", seq));
            msg.setTimestamp(LocalDateTime.now());
            mqProducer.sendAuditMessage(msg);
            success++;
            afterSaleMapper.incrementRetryCount(ticketNo);
        }
        HashMap<String, Integer> map = new HashMap<>();
        map.put("successCount",success);
        map.put("failCount",dto.getTicketNos().size()-success);
        return Result.success("成功重试"+success+"条工单",map);
    }


    @Override
    public void export(HttpServletResponse response) throws IOException {
        // ===================== 1. 设置下载响应头（关键！） =====================
        // 告诉浏览器返回的是xlsx格式的文件
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        // 设置字符编码为UTF-8，防止中文乱码
        response.setCharacterEncoding("utf-8");

        // 处理中文文件名：URL编码 + 兼容主流浏览器
        // 文件名必须编码，否则浏览器会出现中文乱码
        String fileName = URLEncoder.encode("用户售后工单明细表", StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20"); // 替换空格的编码，保证文件名空格正常显示
        // Content-Disposition: attachment 表示以附件形式下载
        response.setHeader("Content-Disposition",
                "attachment;filename*=utf-8''" + fileName + ".xlsx");

        // ===================== 2. 创建Excel工作簿与工作表 =====================
        XSSFWorkbook workbook = new XSSFWorkbook();
        ServletOutputStream outputStream = response.getOutputStream();
        XSSFSheet sheet = workbook.createSheet("用户售后工单表");

        // ===================== 3. 创建表头并设置样式 =====================
        // 定义表头列名
        String[] headers = {
                "工单号", "订单号", "店铺名称",
                "售后类型", "申请原因", "是否有凭证",
                "AI审核结果", "AI置信度",
                "工单状态", "创建时间"
        };
        //创建第0行
        XSSFRow row = sheet.createRow(0);
        // 预定义表头样式：加粗、居中、灰色背景
        CellStyle headerStyle = buildHeaderStyle(workbook);
        // 循环创建表头单元格
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            // 设置列宽：单位是 1/256 个字符宽度，这里设置为20个字符宽
            sheet.setColumnWidth(i, 20 * 256);
        }

        // ===================== 4. 填充业务数据 =====================
        List<AfterSaleOrderListVO> afterSaleOrder = afterSaleMapper.getAfterSaleOrder();
        int rowIndex = 1; // 数据从第1行开始（第0行是表头）
        for (AfterSaleOrderListVO vo : afterSaleOrder) {
            row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(vo.getTicketNo());
            row.createCell(1).setCellValue(vo.getOrderNo());
            row.createCell(2).setCellValue(vo.getStoreName());
            row.createCell(3).setCellValue(vo.getAfterSaleTypeDesc());
            row.createCell(4).setCellValue(vo.getApplyReason());
            row.createCell(5).setCellValue(vo.getHasEvidence() != null && vo.getHasEvidence() ? "有" : "无");
            row.createCell(6).setCellValue(vo.getAiAuditResult());
            row.createCell(7).setCellValue(vo.getAiConfidence() != null ? vo.getAiConfidence().doubleValue() : 0);
            row.createCell(8).setCellValue(vo.getTicketStatusDesc());
            row.createCell(9).setCellValue(vo.getCreatedAt() != null ? vo.getCreatedAt().toString() : "");
        }

        workbook.write(outputStream);
        outputStream.flush();
        log.info("导出为excel文件");
        outputStream.close();
        workbook.close();
    }

    /**
     * 获取客户端真实IP，支持反向代理（Nginx等）
     */
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理的情况，取第一个真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 构建表头样式
     */
    private CellStyle buildHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();

        // 字体加粗
        font.setBold(true);
        // 字体大小
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);

        // 水平居中
        style.setAlignment(HorizontalAlignment.CENTER);
        // 垂直居中
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // 设置背景色（浅灰色）
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 设置边框
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }
}