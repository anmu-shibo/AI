package com.example.shiboaiagent.app;


import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import static org.springframework.ai.chat.memory.ChatMemory.DEFAULT_CONVERSATION_ID;


@Slf4j
@Component
public class PsychiatristApp {

    private static final Logger performanceLogger = LoggerFactory.getLogger("PERFORMANCE");

    private ChatClient chatClient;

    private ChatMemory chatMemory;
    private final String DEFAULT_ADVISOR = "你是一位{occupation}，你会帮助到用户";

    public PsychiatristApp(ChatModel dashScopeChatModel) {

        //初始化聊天内存，最多保存10条消息，这里用的是MessageWindowChatMemory
        chatMemory= MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();
        chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultSystem(DEFAULT_ADVISOR)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .build()
                )
                .build();
    }

    public String doChat(String message, String chatId) {
        String content = chatClient.prompt()
                .system(sp -> sp.param("occupation","医生"))
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(DEFAULT_CONVERSATION_ID, chatId))
                .call()
                .content();

        log.info("chatId: {}，content: {}", chatId, content);
        return content;
    }
}
