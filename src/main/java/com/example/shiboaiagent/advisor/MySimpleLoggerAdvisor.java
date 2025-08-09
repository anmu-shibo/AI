package com.example.shiboaiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

@Slf4j
public class MySimpleLoggerAdvisor implements CallAdvisor, StreamAdvisor {
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {

        logRequest(chatClientRequest);
        ChatClientResponse  chatClientResponse = callAdvisorChain.nextCall(chatClientRequest);
        logResponse(chatClientResponse);
        return chatClientResponse;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        //记录请求日志
        logRequest(chatClientRequest);

        //调用下一个advisor
        Flux<ChatClientResponse> chatClientResponseFlux = streamAdvisorChain.nextStream(chatClientRequest);

        //使用ChatClientMessageAggregator聚合响应流，并在完成时调用logResponse记录日志
        /**
         * ChatClientMessageAggregator() - 消息聚合器，用于处理流式响应
         * aggregateChatClientResponse() - 聚合方法
         * chatClientResponseFlux - 要聚合的响应流
         * this::logResponse - 方法引用，响应完成时的回调函数
         */
        return new ChatClientMessageAggregator().aggregateChatClientResponse(chatClientResponseFlux, this::logResponse);
    }

    @Override
    public String getName() {
        return "日志记录的advisor";
    }

    @Override
    public int getOrder() {
        return 20;
    }

    public void logRequest(ChatClientRequest chatClientRequest) {
        log.info("🔵 AI请求 - 用户消息: {}",
                chatClientRequest.prompt().getInstructions().stream()
                        .filter(msg -> "USER".equals(msg.getMessageType().toString()))
                        .map(msg -> msg.getText())

                        .findFirst().orElse("无用户消息"));
    }
    public void logResponse(ChatClientResponse chatClientResponse) {
        String content = chatClientResponse.chatResponse().getResult().getOutput().getText();
        log.info("🔴 AI响应 - 内容: {}", content.length() > 10000 ? content.substring(0, 10000) + "..." : content);
    }
}
