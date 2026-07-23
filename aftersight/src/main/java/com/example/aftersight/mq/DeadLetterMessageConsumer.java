package com.example.aftersight.mq;

import com.example.aftersight.entity.DeadLetterMessage;
import com.example.aftersight.mapper.DlqMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class DeadLetterMessageConsumer {

    @Resource
    private DlqMapper dlqMapper;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.doc.parse.dlq", durable = "true"),
            exchange = @Exchange(value = "exchange.dlx", type = ExchangeTypes.DIRECT),
            key = "doc.parse.dlq"
    ))
    public void handleDocParseDlq(String message) {
        log.warn("文档解析死信: {}", message);

        DeadLetterMessage dlq = new DeadLetterMessage();
        dlq.setMsgId("DLQ-" + LocalDateTime.now().toString().substring(0, 19));
        dlq.setQueueName("queue.doc.parse");
        dlq.setExchangeName("exchange.knowledge");
        dlq.setRoutingKey("doc.parse");
        dlq.setErrorReason("文档解析异常");
        dlq.setMsgContent(message);
        dlq.setRetryCount(3);
        dlq.setMaxRetry(3);
        dlq.setDlqStatus(0);
        dlq.setErrorTime(LocalDateTime.now());
        dlqMapper.insert(dlq);
    }
}
