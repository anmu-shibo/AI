package com.example.shiboaiagent.app;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;


@Slf4j
@SpringBootTest
class PsychiatristAppTest {

    @Resource
    private PsychiatristApp psychiatristApp;

    @Test
    public void test() {
        String message = "你好，我是史博";
        psychiatristApp.doChat(message, "123");
        message = "你好，你知道我是谁吗";
        psychiatristApp.doChat(message, "123");
        message = "我昨天熬了个夜，对身体有影响吗";
        psychiatristApp.doChat(message, "123");
    }

    @Test
    public void test1() {
        psychiatristApp.doChatWithOutPut("我是史博，我最近有点失眠，但我不知道该怎么做", UUID.randomUUID().toString());
    }
}