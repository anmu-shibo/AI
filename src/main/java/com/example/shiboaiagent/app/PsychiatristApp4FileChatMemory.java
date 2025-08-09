package com.example.shiboaiagent.app;

import com.example.shiboaiagent.advisor.MySimpleLoggerAdvisor;
import com.example.shiboaiagent.chatmemoryrepository.FileChatMemoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import java.util.List;


@Slf4j
// @Component  // 暂时注释掉，避免与 PsychiatristApp 冲突
public class PsychiatristApp4FileChatMemory {
    /** 聊天客户端 - Spring AI的核心组件，负责与AI模型交互 */
    private ChatClient chatClient;

    /** 聊天记忆 - 负责维护对话历史和上下文 */
    private ChatMemory chatMemory;

    /** 默认系统提示词 - 用于基础对话的AI角色设定 */
    private final String DEFAULT_ADVISOR = "你是一位{occupation}，你会帮助到用户";

    /** 诊断专用系统提示词 - 用于生成结构化诊断报告 */
    private final String DIAGNOSTIC_SYSTEM_PROMPT = """
        你是一位专业的{occupation}，请根据患者的症状和描述进行专业分析。
        
        重要要求：
        1. 仔细分析患者提到的具体症状（如失眠、头痛、焦虑等）
        2. 针对症状给出专业、实用的诊断建议
        3. 避免给出通用或模糊的建议
        4. 如果症状严重，建议就医检查
        
        请生成一份诊断报告，格式如下：
        
        标题：患者的诊断报告
        诊断建议：
        1. [针对具体症状的专业建议1]
        2. [针对具体症状的专业建议2] 
        3. [针对具体症状的专业建议3]
        
        示例：如果患者说失眠，应该给出改善睡眠质量、睡前习惯、作息调整等具体建议。
        """;

    /**
     * 构造函数 - 初始化心理医生聊天客户端
     * @param dashScopeChatModel DashScope聊天模型，用于AI对话
     */
    public PsychiatristApp4FileChatMemory(ChatModel dashScopeChatModel) {

        String fileDir = System.getProperty("user.dir") + "/tmp/chat_memory";
        // 1. 初始化文件聊天内存仓库 - 存储对话历史和上下文
        ChatMemoryRepository chatMemoryRepository = new FileChatMemoryRepository(fileDir);

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
     * 诊断结果报告数据结构
     * @param title 诊断报告标题
     * @param suggestions 诊断建议列表
     */
    record DiagnosticResultsReport4FileRepository(String title, List<String> suggestions) {}

    /**
     * 生成诊断报告的聊天方法 - 返回结构化的诊断结果
     * @param message 用户输入的症状或问题描述
     * @param chatId 对话会话ID，用于维护对话上下文
     * @return 包含标题和建议列表的诊断报告
     */
    public DiagnosticResultsReport4FileRepository doChatWithOutPut(String message, String chatId) {
        DiagnosticResultsReport4FileRepository diagnosticResultsReport = chatClient.prompt()
                .system(sp -> sp
                        .text(DIAGNOSTIC_SYSTEM_PROMPT)              // 使用诊断专用的系统提示词
                        .param("occupation", "医生"))                // 设置AI角色
                .user(message)                                   // 用户症状描述
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))  // 指定会话ID
                .call()                                          // 同步调用
                .entity(DiagnosticResultsReport4FileRepository.class);         // 将响应转换为结构化对象

        log.info("诊断报告生成完成: {}", diagnosticResultsReport);
        return diagnosticResultsReport;
    }
}
