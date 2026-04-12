package com.aitripplannerbackend.service.agent.prompt;
import com.aitripplannerbackend.dto.AttractionStepResult;
import com.aitripplannerbackend.dto.TripGenerateRequest;
import com.aitripplannerbackend.dto.WeatherStepResult;
import com.aitripplannerbackend.utils.BudgetValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import static com.aitripplannerbackend.utils.contents.systemContents.ACCOMMODATION_SHARE_OF_TOTAL_BUDGET;
/**
 * 包含一个完整的 JSON Schema，告诉 LLM 输出必须包含哪些字段、什么类型。
 * Schema 和 TripPlanResult 类的结构一一对应，这样反序列化才不会出错。
 *
 * 用户总预算与活动子预算在用户提示中分别给出；summary 中不得把活动子预算说成总预算。
 */
public class ItineraryPrompt {
    public static final String ITINERARY_PROMPT = """
      你是行程规划 Agent。只输出 JSON，不要输出 markdown。
      【输出要求（必须严格遵守）】
          只输出 JSON，不要解释、不要 markdown、不要多余文本
          所有字段必须存在，不允许缺失、null、或类型错误
          所有字符串使用简体中文
          所有金额单位为“人民币元”，且为整数
          严禁输出 schema 之外的字段
          严格输出下面 schema 对应的 JSON：
          {
            "type":"object",
            "required":["city","travelTime","budget","summary","totalEstimatedCost","dailyPlans","tips"],
            "properties":{
              "city":{"type":"string"},
              "travelTime":{"type":"string"},
              "budget":{"type":"integer","minimum":1},
              "summary":{"type":"string"},
              "totalEstimatedCost":{"type":"integer","minimum":0},
              "dailyPlans":{
                "type":"array",
                "minItems":1,
                "items":{
                  "type":"object",
                  "required":["day","theme","estimatedCost","planItems"],
                  "properties":{
                    "day":{"type":"string"},
                    "theme":{"type":"string"},
                    "estimatedCost":{"type":"integer","minimum":0},
                    "planItems":{
                      "type":"array",
                      "minItems":3,
                      "items":{
                        "type":"object",
                        "required":["period","place","activity","transport","expectedCost","mustBookInAdvance"],
                        "properties":{
                          "period":{"type":"string"},
                          "place":{"type":"string"},
                          "activity":{"type":"string"},
                          "transport":{"type":"string"},
                          "expectedCost":{"type":"integer","minimum":0},
                          "mustBookInAdvance":{"type":"boolean"}
                        }
                      }
                    }
                  }
                }
              },
              "tips":{"type":"array","items":{"type":"string"}}
            }
          }
      【约束规则】：
          优先使用“上游提供的景点列表”（如果有）
          结合天气：
              下雨 → 增加室内（博物馆/商场）
              晴天 → 增加室外（公园/景点）
          行程必须“地理上合理”：
              同一天尽量安排相近地点，避免来回跨城/跨区域
          节奏合理：
              上午偏景点
              下午景点或休闲
              晚上餐饮/夜景/商圈
      """;
    /**
     * 用户提示词。
     *
     * 把用户请求、天气结果、景点结果全部序列化成 JSON 拼进去，
     * 让 LLM 能看到所有上下文信息来生成行程。
     *
     * 注意这里用 objectMapper.writeValueAsString() 把 Java 对象转成 JSON 字符串，
     * 这样 LLM 可以"读懂"前两步的结构化输出。
     */
    public static String userPrompt(ObjectMapper objectMapper,
                                    TripGenerateRequest request,
                                    WeatherStepResult weather,
                                    AttractionStepResult attractions) {
        try {
            String weatherJson = objectMapper.writeValueAsString(weather);
            String attractionsJson = objectMapper.writeValueAsString(attractions);
            BigDecimal fullBudget = request.getBudget();
            int tripDays = BudgetValidator.parseDays(request.getTravelTime());
            BigDecimal activityBudget = tripDays <= 1
                    ? fullBudget.setScale(0, RoundingMode.DOWN)
                    : fullBudget.multiply(BigDecimal.valueOf(1 - ACCOMMODATION_SHARE_OF_TOTAL_BUDGET))
                            .setScale(0, RoundingMode.DOWN);
            String activityBudgetExplain = tripDays <= 1
                    ? "（当日往返不安排住宿：全部预算均用于景点门票、餐饮、市内交通；数值与 userTotalBudgetCNY 相同）"
                    : "（仅用于景点门票、餐饮、市内交通的估算上限，不含住宿；不得称为总预算）";
            String userTotalBudget = fullBudget.toPlainString();
            return String.format("""
              请基于以下结构化信息生成完整行程。
              【用户输入】
              city=%s
              travelTime=%s
              userTotalBudgetCNY=%s（用户本次出行总预算，含住宿等整趟花费的上限；JSON 字段 budget 必须等于该整数）
              activityBudgetCNY=%s%s
              【天气步骤输出(JSON)】
              %s
              【景点步骤输出(JSON)】
              %s
              【执行要求】
              1) 优先使用上游提供的景点，不要凭空新增冷门未知地点。
              2) 行程安排需与天气匹配：雨天偏室内，晴天可增加室外。
              3) 同一天尽量安排相近区域，减少往返奔波。
              4) 花费估算使用人民币整数，totalEstimatedCost 必须控制在 activityBudgetCNY 以内。
              5) summary 与 tips 中凡提到「用户预算」「总预算」「整趟花费」等，金额必须指 userTotalBudgetCNY 元；若单独说明不含住宿的活动花费上限，才可写 activityBudgetCNY 元，且不得把后者说成总预算。
              6) 行程最后一天视为返程/离店日：summary 与 tips 不要建议当晚入住酒店，也不要把末日写成需要订房的一晚。
              7) 只返回合法 JSON，不要解释文本。
              """, request.getCity(), request.getTravelTime(), userTotalBudget,
                    activityBudget.toPlainString(), activityBudgetExplain,
                    weatherJson, attractionsJson);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("上游步骤 JSON 序列化失败", e);
        }
    }
}