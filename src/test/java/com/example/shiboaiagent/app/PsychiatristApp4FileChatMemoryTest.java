package com.example.shiboaiagent.app;


import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class PsychiatristApp4FileChatMemoryTest {

    @Resource
    private ChatModel dashScopeChatModel;

    private PsychiatristApp4FileChatMemory psychiatristApp4FileChatMemory;
    @Test
    public void test() {
        // 手动创建实例，避免与其他Component冲突
        psychiatristApp4FileChatMemory = new PsychiatristApp4FileChatMemory(dashScopeChatModel);

        // 使用固定的会话ID，保持对话连续性
        String chatId = UUID.randomUUID().toString();

        var result1 = psychiatristApp4FileChatMemory.doChatWithOutPut("你好，我是史博", chatId);
        System.out.println("第一次诊断：" + result1);

        var result2 = psychiatristApp4FileChatMemory.doChatWithOutPut("你知道我是谁吗", chatId);
        System.out.println("第二次诊断：" + result2);

        var result3 = psychiatristApp4FileChatMemory.doChatWithOutPut("我最近有点失眠", chatId);
        System.out.println("第三次诊断：" + result3);
    }

}