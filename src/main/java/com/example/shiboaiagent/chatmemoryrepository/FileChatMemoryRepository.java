package com.example.shiboaiagent.chatmemoryrepository;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.lang.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于文件系统的聊天记忆仓库实现
 *
 * 功能特点：
 * - 使用 Kryo 序列化框架进行高效的对象序列化/反序列化
 * - 每个对话会话(conversationId)对应一个独立的 .kryo 文件
 * - 支持持久化存储，应用重启后数据不丢失
 * - 自动创建存储目录，管理文件生命周期
 *
 * 文件命名规则：{conversationId}.kryo
 * 存储格式：Kryo 二进制序列化格式
 *
 * @author AI Assistant
 * @since 1.0
 */
public class FileChatMemoryRepository implements ChatMemoryRepository {

    /** 基础存储目录路径 */
    private final String BASE_DIR;

    /** Kryo 序列化实例 - 用于高效的对象序列化和反序列化 */
    private static final Kryo kryo = new Kryo();

    /**
     * 静态初始化块 - 配置 Kryo 序列化器
     */
    static {
        // 允许序列化未注册的类，提高灵活性
        kryo.setRegistrationRequired(false);
        // 设置实例化策略，支持无参构造函数的类
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
    }

    /**
     * 构造函数 - 初始化文件存储仓库
     *
     * @param dir 存储目录路径，如果目录不存在会自动创建
     */
    public FileChatMemoryRepository(String dir) {
        this.BASE_DIR = dir;
        File baseDir = new File(dir);
        // 自动创建存储目录
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }
    }

    /**
     * 保存消息列表到指定对话会话
     * 采用追加模式：新消息会添加到现有消息列表的末尾
     *
     * @param conversationId 对话会话标识符
     * @param messages 要保存的消息列表
     */
    @Override
    public void saveAll(@NonNull String conversationId, @NonNull List<Message> messages) {
        // 直接保存传入的消息列表（替换式保存，而非累积式）
        // Spring AI 的 ChatMemory 层已经处理了消息的累积逻辑
        saveConversation(conversationId, new ArrayList<>(messages));
    }

    /**
     * 根据对话ID查找所有消息
     *
     * @param conversationId 对话会话标识符
     * @return 该会话的所有消息列表，如果会话不存在则返回空列表
     */
    @Override
    @NonNull
    public List<Message> findByConversationId(@NonNull String conversationId) {
        return getOrCreateConversation(conversationId);
    }

    /**
     * 删除指定对话会话的所有数据
     * 物理删除对应的文件
     *
     * @param conversationId 要删除的对话会话标识符
     */
    @Override
    public void deleteByConversationId(@NonNull String conversationId) {
        File file = getConversationFile(conversationId);
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * 获取所有存在的对话会话ID列表
     * 通过扫描存储目录中的 .kryo 文件来获取
     *
     * @return 所有对话会话ID的列表
     */
    @Override
    @NonNull
    public List<String> findConversationIds() {
        List<String> conversationIds = new ArrayList<>();
        File baseDir = new File(BASE_DIR);

        if (baseDir.exists() && baseDir.isDirectory()) {
            // 过滤出所有 .kryo 文件
            File[] files = baseDir.listFiles((dir, name) -> name.endsWith(".kryo"));
            if (files != null) {
                for (File file : files) {
                    // 从文件名中提取 conversationId（移除 .kryo 扩展名）
                    String fileName = file.getName();
                    String conversationId = fileName.substring(0, fileName.lastIndexOf(".kryo"));
                    conversationIds.add(conversationId);
                }
            }
        }

        return conversationIds;
    }

    /**
     * 获取或创建对话消息列表（私有辅助方法）
     * 如果对话文件存在则读取，否则返回空列表
     *
     * @param conversationId 对话会话标识符
     * @return 对话消息列表，如果文件不存在或读取失败则返回空列表
     */
    @NonNull
    private List<Message> getOrCreateConversation(@NonNull String conversationId) {
        File file = getConversationFile(conversationId);
        List<Message> messages = new ArrayList<>();

        if (file.exists()) {
            try (Input input = new Input(new FileInputStream(file))) {
                // 使用 Kryo 反序列化消息列表
                @SuppressWarnings("unchecked")
                List<Message> loadedMessages = (List<Message>) kryo.readObject(input, ArrayList.class);
                messages = loadedMessages;
            } catch (IOException e) {
                System.err.println("Error reading conversation file: " + file.getAbsolutePath());
                e.printStackTrace();
            }
        }
        return messages;
    }

    /**
     * 将消息列表保存到文件（私有辅助方法）
     * 使用 Kryo 序列化框架进行高效的二进制序列化
     *
     * @param conversationId 对话会话标识符
     * @param messages 要保存的消息列表
     */
    private void saveConversation(@NonNull String conversationId, @NonNull List<Message> messages) {
        File file = getConversationFile(conversationId);
        try (Output output = new Output(new FileOutputStream(file))) {
            // 使用 Kryo 序列化消息列表到文件
            kryo.writeObject(output, messages);
        } catch (IOException e) {
            System.err.println("Error saving conversation file: " + file.getAbsolutePath());
            e.printStackTrace();
        }
    }

    /**
     * 根据对话ID生成对应的文件对象（私有辅助方法）
     * 文件命名规则：{conversationId}.kryo
     *
     * @param conversationId 对话会话标识符
     * @return 对应的文件对象
     */
    @NonNull
    private File getConversationFile(@NonNull String conversationId) {
        return new File(BASE_DIR, conversationId + ".kryo");
    }
}
