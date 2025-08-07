package com.example.shiboaiagent.demo.invoke;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Component
public class ChatClientBuilderInvoke implements CommandLineRunner {

    @Resource
    private ChatModel dashScopeChatModel;
    private ChatClient chatClient;
    private String result;

    /**
     * 建造者模式手动构造chatClient
     */
    @PostConstruct
    public void init() {
        //这里使用了建造者模式手动构造了chatClient，并将chatmodel设置成dashScopeChatModel,并将默认系统设置为心理医生
        chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultSystem("你是一位心理医生")
                .build();
    }

    public void executeChat() {
        result = chatClient.prompt().user("你好,我现在有点焦虑").call().content();
        System.out.println("AI回复: " + result);
    }

    @Override
    public void run(String... args) throws Exception {
        executeChat();
    }
}
