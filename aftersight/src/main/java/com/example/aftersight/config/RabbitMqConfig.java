package com.example.aftersight.config;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @RabbitListener(bindings = {
            @QueueBinding(
                    value = @Queue(value = "queue.refund.only", durable = "true"),
                    exchange = @Exchange(value = "exchange.after.sale", type = ExchangeTypes.TOPIC),
                    key = "refund.only.created"),
            @QueueBinding(
                    value = @Queue(value = "queue.refund.return", durable = "true"),
                    exchange = @Exchange(value = "exchange.after.sale", type = ExchangeTypes.TOPIC),
                    key = "refund.return.created"),
            @QueueBinding(
                    value = @Queue(value = "queue.complaint", durable = "true"),
                    exchange = @Exchange(value = "exchange.after.sale", type = ExchangeTypes.TOPIC),
                    key = "complaint.created")
    })
    public void declareBindings() {

    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
