package com.example.shiboaiagent.demo.invoke;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;

public class langchain4jAiInvoke {
    public static void main(String[] args) {
        ChatLanguageModel chatLanguageModel = QwenChatModel.builder()
                .modelName("qwen-plus")
                .apiKey(TestApiKey.API_KEY)
                .build();
        String string = chatLanguageModel.chat("nihao");
        System.out.println(string);
    }
}
