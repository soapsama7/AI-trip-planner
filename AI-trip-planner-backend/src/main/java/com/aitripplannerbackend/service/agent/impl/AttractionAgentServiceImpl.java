package com.aitripplannerbackend.service.agent.impl;

import com.aitripplannerbackend.dto.AttractionStepResult;
import com.aitripplannerbackend.dto.TripGenerateRequest;
import com.aitripplannerbackend.service.agent.AttractionAgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import static com.aitripplannerbackend.service.agent.prompt.AttractionPrompt.ATTRACTION_PROMPT;
import static com.aitripplannerbackend.service.agent.prompt.AttractionPrompt.userPrompt;

/**
 * 景点推荐 Agent 服务实现 —— 调用 LLM 推荐景点。
 *
 *  工作流程：
 * 1. 用 ChatClient 向 LLM（DeepSeek）发送 system prompt + user prompt
 * 2. system prompt 告诉 LLM "你是景点推荐助手，只输出 JSON"，并给出 JSON Schema
 * 3. user prompt 把用户的城市、时间、预算传给 LLM
 * 4. LLM 返回 JSON 字符串，用 ObjectMapper 反序列化成 AttractionStepResult
 *
 * ChatClient 是 Spring AI 提供的 LLM 调用客户端，类似于用 RestTemplate 调接口，
 * 只不过它调的是 AI 大模型的接口。用法：
 * chatClient.prompt()
 *   .system("系统提示词")   // 告诉 AI 它的角色和输出格式要求
 *   .user("用户消息")       // 具体的任务内容
 *   .call()                 // 发送请求
 *   .content();             // 获取 AI 的回复文本
 *
 *  为什么注入的是 ChatClient.Builder 而不是 ChatClient？
 * 因为 Spring AI 推荐每次调用都 build 一个新的 ChatClient 实例，
 * 这样可以为每次调用设置不同的参数（比如 temperature、maxTokens 等）。
 * Builder 是线程安全的，可以共享；ChatClient 实例是用完即弃的。
 */
@Service
@RequiredArgsConstructor
public class AttractionAgentServiceImpl implements AttractionAgentService {

    /** Spring AI 的 ChatClient 构建器，由 Spring 自动注入 */
    private final ChatClient.Builder chatClientBuilder;

    /** Jackson 的 JSON 解析器，用于把 LLM 返回的 JSON 字符串转成 Java 对象 */
    private final ObjectMapper objectMapper;

    /**
     * 调用 LLM 推荐景点。
     *
     * 流程：构建 ChatClient → 发送 prompt → 收到 JSON 文本 → 反序列化
     */
    @Override
    public AttractionStepResult recommendAttractions(TripGenerateRequest request) {
        ChatClient chatClient = chatClientBuilder.build();
        String content;
        try {
            content = chatClient.prompt()
                    .system(ATTRACTION_PROMPT)
                    .user(userPrompt(request))
                    .call()
                    .content();
        } catch (Exception e) {
            throw new IllegalStateException("景点步骤调用模型失败，请检查模型服务或网络连接", e);
        }
        try {
            return objectMapper.readValue(extractJson(content), AttractionStepResult.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("景点步骤 JSON 解析失败", e);
        }
    }

    /**
     * 从 LLM 的回复中提取 JSON 部分。
     *
     * LLM 有时候会在 JSON 前后加一些废话（如 "以下是推荐结果：{...}"），
     * 这个方法找到第一个 '{' 和最后一个 '}'，把中间的 JSON 截取出来。
     * 如果找不到有效的 JSON 包裹，直接抛异常。
     */
    private String extractJson(String content) {
        String text = content == null ? "" : content.trim();
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        throw new IllegalArgumentException("LLM 返回内容不是有效 JSON");
    }
}
