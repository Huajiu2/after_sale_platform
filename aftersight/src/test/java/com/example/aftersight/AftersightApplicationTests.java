package com.example.aftersight;

import com.example.aftersight.controller.TestController;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

@SpringBootTest
class AftersightApplicationTests {
    @Resource
    private TestController tt;

    @Test
    void contextLoads() {
    }


    @Test
    void chat(){
        UserMessage userMessage= UserMessage.from(
                TextContent.from("描述图片"),
                ImageContent.from("https://c-ssl.dtstatic.com/uploads/blog/202303/20/20230320145706_07ca5.thumb.400_0.jpeg")
        );
        tt.chat(userMessage);
    }
}
