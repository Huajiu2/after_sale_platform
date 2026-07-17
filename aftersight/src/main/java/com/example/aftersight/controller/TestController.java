package com.example.aftersight.controller;

import com.example.aftersight.entity.StoreInfo;
import com.example.aftersight.service.TestService;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Component
@RestController
@RequestMapping("/api")
public class TestController {

    @Autowired
    private TestService testService;

    @Resource
    private ChatModel deepChatModel;

    public String chat(UserMessage message) {
        ChatResponse chatResponse = deepChatModel.chat(message);
        AiMessage aiMessage = chatResponse.aiMessage();
        log.info("AI 输出: " + aiMessage.toString());
        return aiMessage.text();
    }

    @GetMapping("/list")
    public List<StoreInfo> testConnect(){
        return testService.testquery();
    }
}
