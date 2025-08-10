package com.example.shiboaiagent.app;


import com.example.shiboaiagent.advisor.MySimpleLoggerAdvisor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;
import static org.springframework.ai.chat.memory.ChatMemory.DEFAULT_CONVERSATION_ID;


/**
 * 心理医生聊天应用 - 基于Spring AI的智能医疗咨询系统
 *
 * 主要功能：
 * 1. 提供基础的医疗咨询对话
 * 2. 生成结构化的诊断报告
 * 3. 维护对话记忆和上下文
 * 4. 记录完整的交互日志
 *
 * 技术特点：
 * - 使用 MessageWindowChatMemory 实现滑动窗口记忆
 * - 支持多用户会话隔离
 * - 集成自定义日志记录顾问
 * - 支持结构化数据输出
 */

@Slf4j
@Component
public class PsychiatristApp {

    /** 聊天客户端 - Spring AI的核心组件，负责与AI模型交互 */
    private ChatClient chatClient;

    /** 聊天记忆 - 负责维护对话历史和上下文 */
    private ChatMemory chatMemory;

    /** 默认系统提示词 - 用于基础对话的AI角色设定 */
    private final String DEFAULT_ADVISOR = "你是一位{occupation}，你会帮助到用户";

    /** 诊断专用系统提示词 - 用于生成结构化诊断报告 */
    private final String DIAGNOSTIC_SYSTEM_PROMPT = """
        你是一位专业的{occupation}，请根据患者的症状和描述进行分析。
        在每次对话后，都要生成一份诊断报告，格式如下：
        
        标题：{用户名}的诊断报告
        诊断建议：
        1. [具体建议1]
        2. [具体建议2]
        3. [具体建议3]
        ...
        
        请确保建议具体、专业且实用。
        """;

    /**
     * 构造函数 - 初始化心理医生聊天客户端
     * @param dashScopeChatModel DashScope聊天模型，用于AI对话
     */
    public PsychiatristApp(ChatModel dashScopeChatModel) {

        // 1. 创建内存聊天记忆仓库 - 负责在内存中存储对话数据
        ChatMemoryRepository chatMemoryRepository = new InMemoryChatMemoryRepository();

        // 2. 初始化滑动窗口聊天内存 - 最多保存10条消息，超出后自动删除最旧的消息
        chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)  // 指定存储仓库
                .maxMessages(10)                             // 设置最大消息数量
                .build();

        // 3. 构建聊天客户端
        chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultSystem(DEFAULT_ADVISOR)  // 设置默认系统提示词
                .defaultAdvisors(
                        // 记忆顾问 - 负责管理对话历史和上下文
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .build(),
                        // 日志顾问 - 负责记录请求和响应日志
                        new MySimpleLoggerAdvisor()
                )
                .build();
    }

    /**
     * 基础聊天方法 - 与AI进行简单对话
     * @param message 用户输入的消息
     * @param chatId 对话会话ID，用于区分不同用户/会话的记忆
     * @return AI的回复内容
     */
    public String doChat(String message, String chatId) {
        String content = chatClient.prompt()
                .system(sp -> sp.param("occupation","医生"))  // 设置AI角色为医生
                .user(message)                               // 用户消息
                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, chatId))  // 指定会话ID
                .call()     // 同步调用
                .content(); // 获取响应内容

         log.info("chatId: {}，content: {}", chatId, content);
        return content;
    }



    /**
     * 诊断结果报告数据结构
     * @param title 诊断报告标题
     * @param suggestions 诊断建议列表
     */
    record DiagnosticResultsReport(String title, List<String> suggestions) {}

    /**
     * 生成诊断报告的聊天方法 - 返回结构化的诊断结果
     * @param message 用户输入的症状或问题描述
     * @param chatId 对话会话ID，用于维护对话上下文
     * @return 包含标题和建议列表的诊断报告
     */
    public DiagnosticResultsReport doChatWithOutPut(String message, String chatId) {
        DiagnosticResultsReport diagnosticResultsReport = chatClient.prompt()
                .system(sp -> sp
                        .text(DIAGNOSTIC_SYSTEM_PROMPT)              // 使用诊断专用的系统提示词
                        .param("occupation", "医生"))              // 设置AI角色
                .user(message)                                   // 用户症状描述
                .advisors(advisorSpec -> advisorSpec.param(CONVERSATION_ID, chatId))  // 指定会话ID
                .call()                                          // 同步调用
                .entity(DiagnosticResultsReport.class);         // 将响应转换为结构化对象

        log.info("诊断报告生成完成: {}", diagnosticResultsReport);
        return diagnosticResultsReport;
    }
}
