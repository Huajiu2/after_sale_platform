package com.example.aftersight.ai;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiServiceFactory {

    @Resource
    private ChatModel gptChatModel;

    @Bean
    public AiService aiService(){

        //会话记忆
        ChatMemory chatMemory= MessageWindowChatMemory.withMaxMessages(10);
        //构造AiService
        AiService aiService = AiServices.builder(AiService.class)
                .chatModel(gptChatModel)
                .chatMemory(chatMemory)
                .build();
        return aiService;
    }


}
