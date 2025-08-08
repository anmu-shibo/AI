# AI智能体--心理医生

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

## 2.怎么实现多轮对话

疑问：默认是使用哪个model？

责任链模式的设计思想

### 2.1 ChatClient

#### 1.ChatClient的创建方式

- 构造器注入方式

注意：很多情况下，ChatClient是一个接口或者需要通过Builder模式创建的类，而不是一个可以直接注入的Spring Bean。所以常用构造器注入

```Java
    private final ChatClient chatClient;

    /**
     * 构造器注入
     * @param chatClientBuilder
     */
    public ChatClientInvoke(ChatClient.Builder chatClientBuilder) {
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
```

- 使用建造者模式

```Java
@Component
public class ChatClientBuilderInvoke implements CommandLineRunner {

    @Resource
    private ChatModel dashScopeChatModel;

    private ChatClient chatClient;
    private String result;

    @PostConstruct
    public void init() {
        //这里使用了建造者模式手动构造了chatClient，并将chatmodel设置成dashScopeChatModel,并将默认系统设置为心理医生
        chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultSystem("你是一位心理医生")
                .build();
    }

    public void executeChat() {
        result = chatClient.prompt()
            .user("你好,我现在有点焦虑")
            .call()
            .content();
        System.out.println("AI回复: " + result);
    }

    @Override
    public void run(String... args) throws Exception {
        executeChat();
    }
}
```

#### 2.ChatClient和ChatModel的区别

- ChatModel只需要注入即可直接调用call方法实现ai对话

```Java
	@Resource
    private ChatModel dashScopeChatModel;
    //调用call方法，里面放着需要问答的内容
    AssistantMessage assistantMessage = dashScopeChatModel.call(new Prompt("你好"))
                .getResult()
                .getOutput();
```

- ChatClient支持更复杂灵活的链式调用

```Java
        result = chatClient.prompt()
            .user("你好,我现在有点焦虑")
            .call()
            .content();
```

#### 3.chatClient多种响应格式

```Java
// ChatClient支持多种响应格式
// 1. 返回 ChatResponse 对象（包含元数据如 token 使用量）
ChatResponse chatResponse = chatClient.prompt()
    .user("Tell me a joke")
    .call()
    .chatResponse();

// 2. 返回实体对象（自动将 AI 输出映射为 Java 对象）
// 2.1 返回单个实体
record ActorFilms(String actor, List<String> movies) {}
ActorFilms actorFilms = chatClient.prompt()
    .user("Generate the filmography for a random actor.")
    .call()
    .entity(ActorFilms.class);

// 2.2 返回泛型集合
List<ActorFilms> multipleActors = chatClient.prompt()
    .user("Generate filmography for Tom Hanks and Bill Murray.")
    .call()
    .entity(new ParameterizedTypeReference<List<ActorFilms>>() {});

// 3. 流式返回（适用于打字机效果）
Flux<String> streamResponse = chatClient.prompt()
    .user("Tell me a story")
    .stream()
    .content();

// 也可以流式返回ChatResponse
Flux<ChatResponse> streamWithMetadata = chatClient.prompt()
    .user("Tell me a story")
    .stream()
    .chatResponse();

```

#### 4.通过使用`sp -> sp.param("occupation","xxx")`动态的更改系统的提示词

```Java
//给ChatClient设置默认参数，动态的更改系统的提示词
        chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultSystem("你是一位{occupation}，你会帮助到用户")
                .build();
//给ChatClient设置默认参数
        Flux<String> stream = chatClient.prompt()
                .system(sp -> sp.param("occupation","医生"))
                .user("你好")
                .stream()
                .content();
        stream.subscribe(System.out::println);
```

### 2.2 拦截器advisor

#### 1. 概念

Spring AI 使用 [Advisors](https://docs.spring.io/spring-ai/reference/api/advisors.html)（顾问）机制来增强 AI 的能力，可以理解为一系列可插拔的拦截器，在调用 AI 前和调用 AI 后可以执行一些额外的操作，比如：

- 前置增强：调用 AI 前改写一下 Prompt 提示词、检查一下提示词是否安全

- 后置增强：调用 AI 后记录一下日志、处理一下返回的结果

#### 2. 原理

<img src="./assets/image-20250808092205368.png" alt="image-20250808092205368" style="zoom: 67%;" />

- Spring AI 框架从用户的 Prompt 创建一个 AdvisedRequest，同时创建一个空的 AdvisorContext 对象，用于传递信息。
- 链中的每个 advisor 处理这个请求，可能会对其进行修改。或者，它也可以选择不调用下一个实体来阻止请求继续传递，这时该 advisor 负责填充响应内容。
- 由框架提供的最终 advisor 将请求发送给聊天模型 ChatModel。
- 聊天模型的响应随后通过 advisor 链传回，并被转换为 AdvisedResponse。后者包含了共享的 AdvisorContext 实例。
- 每个 advisor 都可以处理或修改这个响应。
- 最终的 AdvisedResponse 通过提取 ChatCompletion 返回给客户端。

3. 用法：对话记忆拦截器 MessageChatMemoryAdvisor 可以帮助我们实现多轮对话能؜力，省去了自己维护对话列表的麻烦

```Java
var chatClient = ChatClient.builder(chatModel)
    .defaultAdvisors(
        MessageChatMemoryAdvisor.builder(chatMemory).build(), // 对话记忆 advisor
        QuestionAnswerAdvisor.builder(vectorStore).build()     // RAG 检索增强 advisor
    )
    .build();
```

```Java
var conversationId = "678";

String response = this.chatClient.prompt()
    //
    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
    .user(userText)
    .call()
	.content();
```

#### 3. 注意

拦截器的执行顺序是由getOrder方法决定的

### 2.3 Chat Memory Advisor

想要实现对话记忆功能⁡，可以使用 Spring AI 的 ChatMemoryAdvisor，؜它主要有几种内置的实现方式

- `MessageChatMemoryAdvisor`：将对话历史作为一系列独立的消息添加到提示中，保留原始对话的完整结构，包括每条消息的؜角色标识（用户、助手、系统），**建议使用**

```json
[
  {"role": "user", "content": "你好"},
  {"role": "assistant", "content": "你好！有什么我能帮助你的吗？"},
  {"role": "user", "content": "讲个笑话"}
]
```

- `PromptChatMemoryAdvisor`：将对话历史添加到提示词的系统文本部分，因此可能会؜失去原始的消息边界（如用户对话为用户：用户：你好）

```
以下是之前的对话历史：
用户: 你好
助手: 你好！有什么我能帮助你的吗？
用户: 讲个笑话
现在请继续回答用户的问题。
```

- `VectorStoreChatMemoryAdvisor`：可以用向量数据库来存储检索历史对话

### 2.4 Chat Memory

Chat Memory 负责历史对话的存储，定义了保存消息、查询消息、清空消息历史的方法

<img src="./assets/image-20250808093124395.png" alt="image-20250808093124395" style="zoom:67%;" />

Sprin؜g AI 内置了几⁡种 Chat Memory，可以将对话保存到不同的数据؜源中，比如：

- InMemoryChatMemory：内存存储
- CassandraChatMemory：在 Cassandra 中带有过期时间的持久化存储
- Neo4jChatMemory：在 Neo4j 中没有过期时间限制的持久化存储
- JdbcChatMemory：在 JDBC 中没有过期时间限制的持久化存储

当然也可以؜通过实现 Chat⁡Memory 接口自定义数据源的存储

### 2.5多轮对话的应用开发

1.先初始化一个ChatMemory，并配置最多保存十条信息

```Java
	private ChatMemory chatMemory;
	//初始化聊天内存，最多保存10条消息，这里用的是MessageWindowChatMemory
	chatMemory= MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();
```

2.初始化chatClient，将初始化的chatMemory作为参数配置进MessageChatMemoryAdvisor中，并使用defaultSystem动态配置提示词

```Java
	private final String DEFAULT_ADVISOR = "你是一位{occupation}，你会帮助到用户";	
	private ChatClient chatClient;
	chatClient = ChatClient.builder(dashScopeChatModel)
                .defaultSystem(DEFAULT_ADVISOR)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory)
                                .build()
                )
                .build();
```

3.进行AI对话

```Java
	String content = chatClient.prompt()
        .system(sp -> sp.param("occupation","医生"))
        .user(message)
        .advisors(advisorSpec -> advisorSpec.param(DEFAULT_CONVERSATION_ID, chatId))
        .call()
        .content();
```

4.单元测试

- 如果让chatid不同，那么智能体就无法获取的信息

```Java
@Test
public void test() {
    String message = "你好，我是史博";
    psychiatristApp.doChat(message, "123");
    message = "你好，你知道我是谁吗";
    psychiatristApp.doChat(message, "456");
    message = "你好，你知道我是谁吗";
    psychiatristApp.doChat(message, "458");
}
```

得出的日志如下

![image-20250808113423831](./assets/image-20250808113423831-1754626246713-5.png)

- 设置相同的chatId，但将chatMemory的maxMessages设置为1

```Java
@Test
public void test() {
    String message = "你好，我是史博";
    psychiatristApp.doChat(message, "123");
    message = "你好，你知道我是谁吗";
    psychiatristApp.doChat(message, "123");
    message = "我昨天熬了个夜，对身体有影响吗";
    psychiatristApp.doChat(message, "123");
}
```

依然无法获取上下文

![image-20250808114605398](./assets/image-20250808114605398-1754626238093-3.png)

- 设置相同的chatId，但将chatMemory的maxMessages设置为10

![image-20250808115548454](./assets/image-20250808115548454-1754626232273-1.png)

