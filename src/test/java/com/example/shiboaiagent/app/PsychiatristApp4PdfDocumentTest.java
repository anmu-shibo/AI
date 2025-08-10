package com.example.shiboaiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
class PsychiatristApp4PdfDocumentTest {


    @Resource
    private ChatModel dashScopeChatModel;

    @Resource
    private PsychiatristApp4PdfDocument psychiatristApp4PdfDocument;
    @Test
    void doChatWithRag() {

        // 手动创建实例，避免与其他Component冲突
        psychiatristApp4PdfDocument = new PsychiatristApp4PdfDocument(dashScopeChatModel);

        psychiatristApp4PdfDocument.doChatWithRag("我最近有点感冒，但我不知道该怎么做", "123");
    }
}