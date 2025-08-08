package com.example.shiboaiagent.demo.invoke;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;


@Component
public class ChatClientBuilderInvoke implements CommandLineRunner {

    @Resource
    private ChatModel dashScopeChatModel;
    private ChatClient chatClient;
    private ChatClient chatClient2;
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


        //给C؜hatClient设置默认参数，动态的更改系统的提示词
        chatClient2 = ChatClient.builder(dashScopeChatModel)
                .defaultSystem("你是一位{occupation}，你会帮助到用户")
                .build();
    }

    public void executeChat() {

        result = chatClient.prompt().user("你好,我现在有点焦虑").call().content();
        System.out.println("AI回复: " + result);
        //流式输出
        Flux<String> stream = chatClient.prompt()
                .user("你好,我现在有点焦虑")
                .stream()
                .content();
        stream.subscribe(System.out::println);

    }

    public void executeChat2() {

        //给C؜hatClient设置默认参数
        Flux<String> stream = chatClient2.prompt()
                .system(sp -> sp.param("occupation","医生"))
                .user("你好")
                .stream()
                .content();
        stream.subscribe(System.out::println);
    }

    @Override
    public void run(String... args) throws Exception {
//        executeChat();
//        executeChat2();
    }
}
