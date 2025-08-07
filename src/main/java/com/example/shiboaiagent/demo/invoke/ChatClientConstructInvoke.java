package com.example.shiboaiagent.demo.invoke;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ChatClientConstructInvoke {

    private final ChatClient chatClient;

    /**
     * 构造器注入
     * @param chatClientBuilder
     */
    public ChatClientConstructInvoke(ChatClient.Builder chatClientBuilder) {
        //为什么没有配置chatModel，因为自动注入的chatClientBuilder已经指定了chatModel，这里的应该是dashScopeChatModel
        this.chatClient = chatClientBuilder
                .defaultSystem("你是一位心理医生")
                .build();
    }



    @GetMapping("/ai")
    public String generation(String userInput) {
        return this.chatClient.prompt()
                .user(userInput)
                .call()
                .content();
    }
}


