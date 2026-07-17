package com.example.aftersight.mq;

import com.example.aftersight.ai.AiService;
import com.example.aftersight.mapper.AfterSaleMapper;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import jakarta.annotation.Resource;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AiAuditConsumer {

    @Autowired
    private AiService aiService;

    @Autowired
    private ContentRetriever contentRetriever;

    @Resource
    private AfterSaleMapper afterSaleMapper;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.refund.only", durable = "true"),
            exchange = @Exchange(value = "exchange.after.sale", type = ExchangeTypes.TOPIC),
            key = "refund.only.created"
    ))
    public void handleRefundOnly(AuditMessageDTO message) {
        // 1. 检索相关规则
        List<Content> contents = contentRetriever.retrieve(Query.from(message.getApplyReason()));
        // 2. 组装用户提示词
        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("【工单信息】\n");
        userPrompt.append("订单号：").append(message.getOrderNo()).append("\n");
        userPrompt.append("售后类型：").append(message.getAfterSaleType()).append("\n");
        userPrompt.append("申请原因：").append(message.getApplyReason()).append("\n\n");
        userPrompt.append("【参考规则】\n");
        for (Content content : contents) {
            userPrompt.append(content.textSegment().text()).append("\n\n");
        }
        // 3. 调用 AI 审核
        String result = aiService.chat(userPrompt.toString());
        System.out.println("AI审核结果：" + result);
    }



    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.refund.return", durable = "true"),
            exchange = @Exchange(value = "exchange.after.sale", type = ExchangeTypes.TOPIC),
            key = "refund.return.created"
    ))
    public void handleRefundReturn(AuditMessageDTO message) {
        System.out.println("退货退款：" + message);
    }



    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.complaint", durable = "true"),
            exchange = @Exchange(value = "exchange.after.sale", type = ExchangeTypes.TOPIC),
            key = "complaint.created"
    ))
    public void handleComplaint(AuditMessageDTO message) {
        System.out.println("投诉：" + message);
    }
}