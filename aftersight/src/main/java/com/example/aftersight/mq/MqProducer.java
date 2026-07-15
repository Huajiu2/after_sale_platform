package com.example.aftersight.mq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MqProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;



    public void send(AuditMessageDTO auditMessageDTO){
        String routingkey;
        switch (auditMessageDTO.getAfterSaleType()){
            case 1: {
                routingkey = "refund.only.created";
                break;
            }
            case 2:{
                routingkey="refund.return.created";
                break;
            }
            case 3:{
                routingkey="complaint.created";
                break;
            }
            default: throw new RuntimeException("售后工单状态错误");

        }
        rabbitTemplate.convertAndSend("exchange.after.sale", routingkey, auditMessageDTO);
    }


}
