package com.example.shiboaiagent.demo.invoke;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 阿里云DashScope API客户端
 * 使用hutool工具类发送请求
 */
public class HttpInvoke {

    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
    private String apiKey;

    private HttpInvoke(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 发送文本生成请求
     * @param systemMessage 系统消息
     * @param userMessage 用户消息
     * @return API响应
     */
    public String sendTextGenerationRequest(String systemMessage, String userMessage) {
        // 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.set("model", "qwen-plus");

        // 构建input部分
        JSONObject input = new JSONObject();
        List<JSONObject> messages = new ArrayList<>();

        // 添加系统消息
        JSONObject systemMsg = new JSONObject();
        systemMsg.set("role", "system");
        systemMsg.set("content", systemMessage);
        messages.add(systemMsg);

        // 添加用户消息
        JSONObject userMsg = new JSONObject();
        userMsg.set("role", "user");
        userMsg.set("content", userMessage);
        messages.add(userMsg);

        input.set("messages", messages);
        requestBody.set("input", input);

        // 构建parameters部分
        JSONObject parameters = new JSONObject();
        parameters.set("result_format", "message");
        requestBody.set("parameters", parameters);

        // 发送HTTP请求
        HttpResponse response = HttpRequest.post(API_URL)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestBody.toString())
                .execute();

        return response.body();
    }

    /**
     * 使用默认系统消息发送请求
     * @param userMessage 用户消息
     * @return API响应
     */
    public String sendTextGenerationRequest(String userMessage) {
        return sendTextGenerationRequest("You are a helpful assistant.", userMessage);
    }

    /**
     * 示例用法
     */
    public static void main(String[] args) {
        // 从环境变量获取API密钥
        String apiKey = TestApiKey.API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("请设置环境变量 DASHSCOPE_API_KEY");
            return;
        }

        HttpInvoke client = new HttpInvoke(apiKey);

        try {
            String response = client.sendTextGenerationRequest("你是谁？");
            System.out.println("API响应: " + response);

            // 解析响应（可选）
            JSONObject responseJson = JSONUtil.parseObj(response);
            System.out.println("响应状态: " + responseJson.getStr("status"));

        } catch (Exception e) {
            System.err.println("请求失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


