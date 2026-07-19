package com.example.aftersight.mq;

import com.example.aftersight.ai.AiService;
import com.example.aftersight.common.AiAuditResult;
import com.example.aftersight.entity.AfterSaleOrder;
import com.example.aftersight.entity.AiAuditLog;
import com.example.aftersight.entity.OrderInfo;
import com.example.aftersight.mapper.AfterSaleMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
public class AiAuditConsumer {

    @Autowired
    private AiService aiService;

    @Autowired
    private ContentRetriever contentRetriever;

    @Resource
    private AfterSaleMapper afterSaleMapper;

    @Resource
    private ObjectMapper objectMapper;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.refund.only", durable = "true"),
            exchange = @Exchange(value = "exchange.after.sale", type = ExchangeTypes.TOPIC),
            key = "refund.only.created"
    ))
    public void handleRefundOnly(AuditMessageDTO message) {
        processAudit(message);
    }


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.refund.return", durable = "true"),
            exchange = @Exchange(value = "exchange.after.sale", type = ExchangeTypes.TOPIC),
            key = "refund.return.created"
    ))
    public void handleRefundReturn(AuditMessageDTO message) {
        processAudit(message);
    }


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.complaint", durable = "true"),
            exchange = @Exchange(value = "exchange.after.sale", type = ExchangeTypes.TOPIC),
            key = "complaint.created"
    ))
    public void handleComplaint(AuditMessageDTO message) {
        processAudit(message);
    }

    private void processAudit(AuditMessageDTO message) {
        try {
            // 1. 检索相关规则
            List<Content> contents = contentRetriever.retrieve(Query.from(message.getApplyReason()));

            // 2. 组装提示词
            StringBuilder userPrompt = new StringBuilder();
            userPrompt.append("【工单信息】\n");
            // 查订单信息
            OrderInfo orderInfo = afterSaleMapper.getOrder(message.getOrderNo());
            if (orderInfo != null) {
                userPrompt.append("商品名称：").append(orderInfo.getProductName()).append("\n");
                userPrompt.append("商品规格：").append(orderInfo.getProductSpec()).append("\n");
                userPrompt.append("实付金额：").append(orderInfo.getPayAmount()).append("元\n");
                userPrompt.append("下单时间：").append(orderInfo.getOrderTime()).append("\n");
            }
            userPrompt.append("订单号：").append(message.getOrderNo()).append("\n");
            userPrompt.append("售后类型：").append(message.getAfterSaleType()).append("\n");
            userPrompt.append("申请原因：").append(message.getApplyReason()).append("\n\n");
            userPrompt.append("【参考规则】\n");
            for (Content content : contents) {
                userPrompt.append(content.textSegment().text()).append("\n\n");
            }

            log.info(userPrompt.toString());
            // 3. 调用 AI
            long start = System.currentTimeMillis();
            String result = aiService.chat(userPrompt.toString());
            long latency = System.currentTimeMillis() - start;

            // 4. 解析 JSON
            AiAuditResult auditResult = objectMapper.readValue(result, AiAuditResult.class);

            // 5. 更新工单
            AfterSaleOrder afterorder = afterSaleMapper.getByTicketNo(message.getTicketNo());
            if (afterorder == null) {
                log.error("工单不存在: {}", message.getTicketNo());
                return;
            }

            Integer ticketStatus = auditResult.getConfidence() >= 85 ? 1 : 2;
            Integer aiAuditStatus = auditResult.getConfidence() >= 85 ? 1 : 2;

            afterorder.setAiAuditResult(objectMapper.writeValueAsString(auditResult));
            afterorder.setAiConfidence(BigDecimal.valueOf(auditResult.getConfidence()));
            afterorder.setAiAuditStatus(aiAuditStatus);
            afterorder.setTicketStatus(ticketStatus);
            afterSaleMapper.updateAiAudit(afterorder);

            // 6. 写日志
            AiAuditLog auditLog = new AiAuditLog();
            auditLog.setTicketId(afterorder.getId());
            auditLog.setTicketNo(message.getTicketNo());
            auditLog.setLlmModel("gpt-5.5");
            auditLog.setLlmPrompt(userPrompt.toString());
            auditLog.setLlmResponse(result);
            auditLog.setLlmLatencyMs((int) latency);
            // 召回切片来源文档名
            String chunkIds = contents.stream()
                    .map(c -> c.textSegment().metadata().getString("file_name"))
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .collect(java.util.stream.Collectors.joining(","));
            auditLog.setRagChunkIds(chunkIds);
            auditLog.setAuditConclusion(auditResult.getConclusion());
            auditLog.setConfidence(BigDecimal.valueOf(auditResult.getConfidence()));
            auditLog.setSuggestedAction(auditResult.getSuggestedAction());
            afterSaleMapper.insertAiAuditLog(auditLog);

            log.info("AI审核完成: ticketNo={}, conclusion={}, confidence={}",
                    message.getTicketNo(), auditResult.getConclusion(), auditResult.getConfidence());

        } catch (Exception e) {
            log.error("AI审核失败: ticketNo={}", message.getTicketNo(), e);
        }
    }
}