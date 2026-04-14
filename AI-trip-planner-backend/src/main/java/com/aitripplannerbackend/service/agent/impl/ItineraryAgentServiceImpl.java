package com.aitripplannerbackend.service.agent.impl;

import com.aitripplannerbackend.dto.AttractionStepResult;
import com.aitripplannerbackend.dto.TripGenerateRequest;
import com.aitripplannerbackend.dto.TripPlanResult;
import com.aitripplannerbackend.dto.WeatherStepResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.aitripplannerbackend.service.agent.ItineraryAgentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import static com.aitripplannerbackend.service.agent.prompt.ItineraryPrompt.ITINERARY_PROMPT;
import static com.aitripplannerbackend.service.agent.prompt.ItineraryPrompt.userPrompt;
import static com.aitripplannerbackend.utils.contents.systemContents.ACCOMMODATION_SHARE_OF_TOTAL_BUDGET;
import static com.aitripplannerbackend.utils.contents.systemContents.MIN_NIGHTLY_PRICE;

/**
 * 行程规划 Agent 服务实现 —— 调用 LLM 生成完整的每日行程。
 *
 * 它接收前两步的输出（天气 + 景点），连同用户请求一起发给 LLM，
 * 让 LLM 综合这些信息生成一份完整的、结构化的旅行行程。
 *
 *     为什么要把天气和景点的结果也传给 LLM？
 * - 天气：LLM 可以根据天气预报调整安排（下雨安排室内、晴天安排户外）
 * - 景点：LLM 从推荐的景点中挑选并合理安排到每天，避免凭空编造景点
 * - 预算：多日时 LLM 基于活动预算（总预算 × 65%）规划；单日往返时活动预算等于总预算。住宿由独立逻辑按 35% 分配（仅前 n−1 天，末日不设住宿）
 *
 * system prompt 给了一个详细的 JSON Schema，要求 LLM 严格按格式输出。
 * 这种做法称为 "结构化输出"（Structured Output）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ItineraryAgentServiceImpl implements ItineraryAgentService {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper;
    @Qualifier("amapWebClient")
    private final WebClient amapWebClient;

    @Value("${amap.web-key:}")
    private String amapWebKey;

    /**
     * 调用 LLM 生成完整行程。
     *
     * 1. 构建 ChatClient
     * 2. 发送 system prompt（角色 + JSON Schema）+ user prompt（所有上下文信息）
     * 3. LLM 返回 JSON → 反序列化为 TripPlanResult
     */
    @Override
    public TripPlanResult generateItinerary(TripGenerateRequest request,
                                           WeatherStepResult weather,
                                           AttractionStepResult attractions) {
        ChatClient chatClient = chatClientBuilder.build();
        String content;
        try {
            content = chatClient.prompt()
                    .system(ITINERARY_PROMPT)
                    .user(userPrompt(objectMapper,request, weather, attractions))
                    .call()
                    .content();
        } catch (Exception e) {
            throw new IllegalStateException("行程步骤调用模型失败，请检查模型服务或网络连接", e);
        }
        try {
            TripPlanResult plan = objectMapper.readValue(extractJson(content), TripPlanResult.class);
            plan.setBudget(request.getBudget().intValue());
            enrichAccommodation(request, plan);
            addAccommodationToTotalCost(plan);
            if (estimateTripDays(request.getTravelTime()) <= 1) {
                plan.setAccommodationNote("因为旅行只有一天（视为当日往返），因此未规划住宿。");
            }
            return plan;
        } catch (Exception e) {
            throw new IllegalArgumentException("行程步骤 JSON 解析失败", e);
        }
    }

    /**
     * 从 LLM 回复中提取 JSON。
     * 和 AttractionAgentServiceImpl 中的逻辑一样：
     * 找第一个 '{' 和最后一个 '}'，截取中间部分。
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

    /**
     * 用「各时段 expectedCost 之和 + 当晚住宿」重算每日与总花费，覆盖 LLM 可能不一致的 estimatedCost / totalEstimatedCost。
     */
    private void addAccommodationToTotalCost(TripPlanResult plan) {
        if (plan == null || plan.getDailyPlans() == null) {
            return;
        }
        int grand = 0;
        for (TripPlanResult.DailyPlan day : plan.getDailyPlans()) {
            int itemsSum = sumExpectedCostOfPlanItems(day);
            int nightPrice = 0;
            if (day.getStay() != null && day.getStay().getPricePerNight() != null) {
                nightPrice = day.getStay().getPricePerNight();
            }
            int dailyTotal = itemsSum + nightPrice;
            day.setEstimatedCost(dailyTotal);
            grand += dailyTotal;
        }
        plan.setTotalEstimatedCost(grand);
    }

    /**
     *      计算每天各个时段的开销（不包含住宿）
     */
    private static int sumExpectedCostOfPlanItems(TripPlanResult.DailyPlan day) {
        if (day == null || day.getPlanItems() == null) {
            return 0;
        }
        return day.getPlanItems().stream()
                .filter(Objects::nonNull)
                .mapToInt(p -> p.getExpectedCost() == null ? 0 : p.getExpectedCost())
                .sum();
    }

    private void enrichAccommodation(TripGenerateRequest request, TripPlanResult plan) {
        if (plan == null || plan.getDailyPlans() == null || plan.getDailyPlans().isEmpty()) {
            return;
        }
        List<TripPlanResult.DailyPlan> days = plan.getDailyPlans();
        for (int i = 0; i < days.size(); i++) {
            if (i == days.size() - 1) {
                days.get(i).setStay(null);
                continue;
            }
            days.get(i).setStay(recommendStay(request, days.get(i)));
        }
    }

    private TripPlanResult.Stay recommendStay(TripGenerateRequest request, TripPlanResult.DailyPlan dailyPlan) {
        String city = request.getCity();
        String anchorPlace = pickAnchorPlace(dailyPlan);
        int estimatedNightlyPrice = estimateNightlyPrice(request);
        if (amapWebKey == null || amapWebKey.isBlank()) {
            return fallbackStay(anchorPlace, estimatedNightlyPrice);
        }
        try {
            AmapPoiResponse poiResp = amapWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/v5/place/text")
                            .queryParam("key", amapWebKey.trim())
                            .queryParam("keywords", "酒店")
                            .queryParam("region", city)
                            .queryParam("city_limit", "true")
                            .queryParam("sortrule", "distance")
                            .queryParam("show_fields", "business")
                            .queryParam("page_size", 5)
                            .build())
                    .retrieve()
                    .bodyToMono(AmapPoiResponse.class)
                    .block();

            if (poiResp == null || poiResp.getPois() == null || poiResp.getPois().isEmpty()) {
                return fallbackStay(anchorPlace, estimatedNightlyPrice);
            }
            /*
                .findFirst()
                在过滤后的流里取第一个元素（Stream 里元素的顺序就是高德返回列表的顺序；你前面请求里一般是按 sortrule 排好的，例如按距离）。
                .orElse(null)
                如果过滤后流是空的（没有任何合法酒店），findFirst() 得到的是空的 Optional，这里就返回 null，后面代码会走 if (top == null) 用兜底住宿
             */
            AmapPoi top = poiResp.getPois().stream()
                    .filter(Objects::nonNull)
                    .filter(p -> p.getName() != null && !p.getName().isBlank())
                    .findFirst()
                    .orElse(null);
            if (top == null) {
                return fallbackStay(anchorPlace, estimatedNightlyPrice);
            }
            Integer price = parseInt(top.getBizExt() == null ? null : top.getBizExt().getCost(), 0);
            Integer distance = parseInt(top.getDistance(), 0);
            return TripPlanResult.Stay.builder()
                    .name(top.getName())
                    .address(defaultText(top.getAddress(), "地址待补充"))
                    .pricePerNight(price <= 0 ? estimatedNightlyPrice : price)
                    .distanceToLastSpot(distance <= 0 ? null : distance)
                    .reason("靠近当日活动区域，适合收尾入住")
                    .build();
        } catch (Exception ex) {
            log.warn("高德住宿查询失败，将使用兜底建议 city={}, msg={}", city, ex.getMessage());
            return fallbackStay(anchorPlace, estimatedNightlyPrice);
        }
    }

    private String pickAnchorPlace(TripPlanResult.DailyPlan dailyPlan) {
        if (dailyPlan == null || dailyPlan.getPlanItems() == null || dailyPlan.getPlanItems().isEmpty()) {
            return "";
        }
        TripPlanResult.PlanItem last = dailyPlan.getPlanItems().get(dailyPlan.getPlanItems().size() - 1);
        return defaultText(last.getPlace(), "");
    }

    private TripPlanResult.Stay fallbackStay(String anchorPlace, int estimatedNightlyPrice) {
        return TripPlanResult.Stay.builder()
                .name("商圈舒适型住宿（建议自选）")
                .address(anchorPlace.isBlank() ? "优先选择地铁站附近住宿" : ("建议优先选择靠近 " + anchorPlace + " 的住宿"))
                .pricePerNight(estimatedNightlyPrice)
                .distanceToLastSpot(null)
                .reason("参考价按本次预算与天数估算，建议在地图里按该价格区间筛选")
                .build();
    }

    /**
     * 估算每晚住宿参考价（元）。
     * 住宿夜数 = 行程日历天数 − 1（最后一天离返程，不安排住宿），据此均摊住宿预算池。
     */
    private int estimateNightlyPrice(TripGenerateRequest request) {
        int tripDays = estimateTripDays(request.getTravelTime());
        if (tripDays <= 0) {
            tripDays = 1;
        }
        int stayNights = Math.max(0, tripDays - 1);
        if (stayNights <= 0) {
            return MIN_NIGHTLY_PRICE;
        }
        BigDecimal budget = request.getBudget() == null ? BigDecimal.ZERO : request.getBudget();
        int total = budget.intValue();
        int accommodationPoolTotal = Math.round(total * ACCOMMODATION_SHARE_OF_TOTAL_BUDGET);
        int nightly = Math.round((float) accommodationPoolTotal / stayNights);
        if (nightly < MIN_NIGHTLY_PRICE) {
            return MIN_NIGHTLY_PRICE;
        }
        // 单晚参考价上限（避免天价展示）
        return Math.min(nightly, 1200);
    }

    private int estimateTripDays(String travelTime) {
        try {
            if (travelTime == null || !travelTime.contains("~")) {
                return 1;
            }
            String[] parts = travelTime.split("~");
            if (parts.length != 2) {
                return 1;
            }
            LocalDate start = LocalDate.parse(parts[0].trim());
            LocalDate end = LocalDate.parse(parts[1].trim());
            long days = ChronoUnit.DAYS.between(start, end) + 1;
            return (int) Math.max(days, 1);
        } catch (Exception ignore) {
            return 1;
        }
    }

    private String defaultText(String val, String fallback) {
        return (val == null || val.isBlank()) ? fallback : val;
    }

    private Integer parseInt(String raw, Integer fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    // -------------------------------------------------------------------------
    // 高德「地点搜索 /v5/place/text」响应体 —— 仅用于 WebClient JSON 反序列化，
    // 不是对外 API 的 DTO。未声明的 JSON 字段由 @JsonIgnoreProperties(ignoreUnknown=true) 忽略。
    // -------------------------------------------------------------------------

    /**
     * 关键词搜索接口返回的根对象，对应 {@code .bodyToMono(AmapPoiResponse.class)}。
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AmapPoiResponse {
        /** 接口调用状态（如 "1" 表示成功）；当前业务逻辑未强依赖此字段 */
        private String status;
        /** 命中的 POI 列表；住宿推荐里取第一条有名称的酒店 */
        private List<AmapPoi> pois;
    }

    /**
     * 单个兴趣点（POI）。本类请求里 keywords=酒店，故列表元素多为酒店。
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AmapPoi {
        /** 酒店或场所名称 */
        private String name;
        /** 详细地址 */
        private String address;
        /** 与搜索参照的距离（米），高德以字符串返回；用于展示「距上一景点约 X 米」 */
        private String distance;
        /**
         * 扩展业务字段；JSON 里叫 {@code biz_ext}，故用 {@link JsonProperty} 映射到 Java 驼峰字段。
         * 其中 {@link AmapBizExt#getCost()} 可解析为每晚参考价（元）。
         */
        @JsonProperty("biz_ext")
        private AmapBizExt bizExt;
    }

    /**
     * {@link AmapPoi#bizExt} 对应的子对象结构；接口可能还带其它键，此处只声明用到的字段。
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AmapBizExt {
        /** 参考价（多为整数元的字符串）；解析失败或 ≤0 时用 {@link #estimateNightlyPrice} 的估算值 */
        private String cost;
    }
}
