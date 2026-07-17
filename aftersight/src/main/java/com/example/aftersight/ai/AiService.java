package com.example.aftersight.ai;

import dev.langchain4j.service.SystemMessage;

public interface AiService {

    @SystemMessage(fromResource = "system-prompt.txt")
    String chat(String message);
}
