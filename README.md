# AI智能体

## 1.AI大模型调用的四种方式

### 1.1 SDK调用

- 需要pom.xml导入DashScope包

```java 
		<dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>dashscope-sdk-java</artifactId>
            <version>2.21.1</version>
        </dependency>
```

- 在官方API文档中复制示例代码

```java
public static GenerationResult callWithMessage() throws ApiException, NoApiKeyException, InputRequiredException {
        Generation gen = new Generation();
        Message systemMsg
                = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("You are a helpful assistant.")
                .build();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content("你是谁？")
                .build();
        GenerationParam param = GenerationParam.builder()
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey(TestApiKey.API_KEY)
                // 此处以qwen-plus为例，可按需更换模型名称。模型列表：https://help.aliyun.com/zh/model-studio/getting-started/models
                .model("qwen-plus")
                .messages(Arrays.asList(systemMsg, userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();
        return gen.call(param);
    }
    public static void main(String[] args) {
        try {
            GenerationResult result = callWithMessage();
            System.out.println(JsonUtils.toJson(result));
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            // 使用日志框架记录异常信息
            System.err.println("An error occurred while calling the generation service: " + e.getMessage());
        }
        System.exit(0);
    }
```

### 1.2 Http调用

- 根据官方文档中给出的curl指令，使用cursor转化成Java的Hutool工具类的请求代码

**Hutool工具类需要去了解学习**

```Java
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
```

### 1.3 SpringAI调用

- 根据[官方提供](https://java2ai.com/docs/1.0.0-M6.1/models/qwq/?spm=5176.29160081.0.0.2856aa5cAzHAE6)的文档，首先需要在application.yml中配置

```Java
spring：  
	ai:
    dashscope:
      api-key: sk-xxxxxxxx
      chat:
        options:
          model: qwen-plus
```

- 导入pom文件

```Java
        <dependency>
            <groupId>com.alibaba.cloud.ai</groupId>
            <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
            <version>1.0.0.2</version>
        </dependency>
```

- 使用`@Component`将类注册到容器中，然后使用`@Resource`将`dashScopeChatModel`注入进来即可直接调用

```Java
@Component
public class SpringAiInvoke implements CommandLineRunner {

    @Resource
    //在配置文件中配置了dashScope，SpringAi会自动查找，然后注入
    private ChatModel dashScopeChatModel;
    private ChatModel ollamChatModel;

    @Override
    public void run(String... args) throws Exception {
        AssistantMessage assistantMessage = dashScopeChatModel.call(new Prompt("你好"))
                .getResult()
                .getOutput();
        System.out.println(assistantMessage.getText());
    }
}
```

### 1.4 Langchain4J调用

- 根据官方文档导入pom文件，这个jar仅可支持dashscope调用

```Java
<!-- https://mvnrepository.com/artifact/dev.langchain4j/langchain4j-community-dashscope -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-community-dashscope</artifactId>
    <version>1.0.0-beta2</version>
</dependency>
```

- 可直接使用`QwenChatModel.builder`初始化通义千问大模型，也可

```Java
	public static void main(String[] args) {
        ChatLanguageModel chatLanguageModel = QwenChatModel.builder()
                .modelName("qwen-plus")
                .apiKey(TestApiKey.API_KEY)
                .build();
        String string = chatLanguageModel.chat("nihao");
        System.out.println(string);
    }
```

