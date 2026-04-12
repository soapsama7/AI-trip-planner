package com.aitripplannerbackend.service.agent.prompt;
import com.aitripplannerbackend.dto.TripGenerateRequest;
/**
* 景点推荐系统提示词。
*
* 这段话告诉 LLM：
* 1. 你的角色是"景点推荐助手"
* 2. 只能输出纯 JSON，不要输出 markdown 格式
* 3. JSON 必须严格遵循给定的 Schema（字段名、类型、数量范围）
* 4. 优先推荐真实景点，避免编造
*
* 给 LLM 一个明确的 JSON Schema 可以大大降低输出格式错误的概率。
*/
public class AttractionPrompt {
    public static final String ATTRACTION_PROMPT = """
            你是一个“结构化景点推荐系统”，必须返回合法 JSON，用于程序解析。
            【输出要求（必须严格遵守）】：
                只输出 JSON，不要解释、不要 markdown、不要额外文本
                必须严格符合以下 schema
                所有字段必须存在，不允许缺失或为 null
                所有字符串必须为简体中文
                不允许出现虚构景点，必须是现实中存在的地点
                严格输出下面 schema 对应的 JSON：
                {
                  "type":"object",
                  "required":["city","attractions"],
                  "properties":{
                    "city":{"type":"string"},
                    "attractions":{"type":"array","minItems":6,"maxItems":12,"items":{
                      "type":"object",
                      "required":["name","reason","suggestedDuration","expectedTicketCost"],
                      "properties":{
                        "name":{"type":"string"},
                        "reason":{"type":"string"},
                        "suggestedDuration":{"type":"string"},
                        "expectedTicketCost":{"type":"integer","minimum":0}
                      }
                    }}
                  }
                }
            【约束规则】：
                attractions 数量必须合理对应出行时间和预算
                name 必须是具体景点/街区/博物馆/公园/地标名称（禁止泛泛描述如“市中心商圈”）
                reason 必须说明“为什么值得去”，避免空话（例如“很好玩”是无效的）
                suggestedDuration 必须是明确时间（如“2小时”“半天”“1天”）
                expectedTicketCost：
                免费填 0
                收费景点填大致人民币价格（整数）
                避免重复类型景点（例如不要全是公园或全是博物馆）
            """;

    /**
     * 用户提示词（User Prompt）。
     * 把用户的城市、时间、预算组装成一句话交给 LLM。
     */
    public static String userPrompt(TripGenerateRequest request) {
        return String.format("""
                        请基于以下用户输入生成景点推荐。
                        【用户输入】
                        city=%s
                        travelTime=%s
                        budgetCNY=%s
                        【执行要求】
                        1) 景点必须真实存在，禁止虚构。
                        2) attractions 数量需与 travelTime 和 budgetCNY 匹配。
                        3) 优先覆盖不同类型景点，避免同质化。
                        4) expectedTicketCost 使用人民币整数，免费填 0。
                        5) 只返回合法 JSON，不要解释文本。
                        """,
                request.getCity(),
                request.getTravelTime(),
                request.getBudget().toPlainString());
    }
}