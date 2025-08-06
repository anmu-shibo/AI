package com.example.shiboaiagent.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * SpringAI调用大模型
 */
@Component
public class SpringAiInvoke implements CommandLineRunner {

    @Resource
    private ChatModel dashScopeChatModel;
    private ChatModel ollamChatModel;

    @Override
    public void run(String... args) throws Exception {
        AssistantMessage assistantMessage = dashScopeChatModel.call(new Prompt("你好"))
                .getResult()
                .getOutput();
        System.out.println(assistantMessage.getText());
    }
}
