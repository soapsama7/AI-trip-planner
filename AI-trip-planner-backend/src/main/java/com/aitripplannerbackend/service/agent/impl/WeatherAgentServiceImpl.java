package com.aitripplannerbackend.service.agent.impl;

import com.aitripplannerbackend.dto.TripGenerateRequest;
import com.aitripplannerbackend.dto.WeatherStepResult;
import com.aitripplannerbackend.dto.QWeatherJsonModels.CityLookupResponse;
import com.aitripplannerbackend.dto.QWeatherJsonModels.DailyForecast;
import com.aitripplannerbackend.dto.QWeatherJsonModels.DailyForecastResponse;
import com.aitripplannerbackend.service.agent.WeatherAgentService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import static com.aitripplannerbackend.utils.contents.dtoContents.DAY_FMT;

@Service
public class WeatherAgentServiceImpl implements WeatherAgentService {

    /**
     * 和风天气专用的 HTTP 客户端，在 QWeatherConfig 中创建的 Bean。
     * 已经配好了 baseUrl（和风的 API Host）和请求头（API Key），
     */
    private final WebClient qweatherWebClient;

    /**
     * 构造函数注入。
     * @Qualifier("qweatherWebClient") 的作用：项目里有多个 WebClient Bean
     * （比如 LLM 用的也有一个），加 @Qualifier 指定要注入名字叫 "qweatherWebClient" 的那个。
     */
    public WeatherAgentServiceImpl(@Qualifier("qweatherWebClient") WebClient qweatherWebClient) {
        this.qweatherWebClient = qweatherWebClient;
    }

    @Override
    public WeatherStepResult generateWeather(TripGenerateRequest request) {
        // 1. 解析出行日期：前端保证格式为 "2026-04-10~2026-04-13"，直接按 ~ 进行字符串分割
        String[] parts = request.getTravelTime().split("~");
        LocalDate start = LocalDate.parse(parts[0]);
        LocalDate end = LocalDate.parse(parts[1]);
        long tripDays = end.toEpochDay() - start.toEpochDay() + 1;

        /*
            ========== 2. 通过 GeoAPI 拿到城市的 locationId ==========
            和风天气的天气接口不接受城市名，只接受 locationId（如 "101010100"）
            所以必须先调一次城市搜索接口，如把 "北京" 转成 "101010100"
         */
        String locationId = lookupCityId(request.getCity());

        /*
            ========== 3. 拉取逐日预报 ==========
            和风天气的预报接口按天数分档：3d/7d/15d/30d，但前端最大只有7d
            根据行程天数选一个够用的最小档位，避免请求多余数据
         */
        String segment = tripDays <= 3 ? "3d" : "7d" ;
        List<DailyForecast> allDaily = fetchForecast(locationId, segment);

        /*
            ========== 4. 只保留出行区间内的预报 ==========
            比如请求了 7 天预报，但行程只有 4 天，就过滤掉多余的日期（在开始日期之前和结束日期之后的）
        */
        List<DailyForecast> inRange = allDaily.stream()
                .filter(d -> {
                    LocalDate date = LocalDate.parse(d.fxDate);
                    return !date.isBefore(start) && !date.isAfter(end);
                })
                .toList();

        /*
            ========== 5. 转成前端需要的 DTO ==========
            和风 API 返回的字段格式（比如日期、温度等都是字符串）不适合直接给前端，
            这里转成 WeatherStepResult.DailyWeather，日期格式化为"4月10日 星期五"这样的中文
        */
        List<WeatherStepResult.DailyWeather> daily = inRange.stream()
                .map(d -> WeatherStepResult.DailyWeather.builder()
                        .day(LocalDate.parse(d.fxDate).format(DAY_FMT))
                        .weather(d.textDay.equals(d.textNight) ? d.textDay : d.textDay + " / " + d.textNight)
                        .tempMin(Integer.parseInt(d.tempMin))
                        .tempMax(Integer.parseInt(d.tempMax))
                        .build())
                .toList();

        // ========== 6. 生成简要摘要和行李建议 ==========

        // 如果没有值，就用默认的0
        int lo = daily.stream().mapToInt(WeatherStepResult.DailyWeather::getTempMin).min().orElse(0);
        int hi = daily.stream().mapToInt(WeatherStepResult.DailyWeather::getTempMax).max().orElse(0);

        String summary = String.format("%s %s～%s（%d天）：气温 %d～%d°C，数据来自和风天气。",
                request.getCity(), start, end, tripDays, lo, hi);

        List<String> tips = new ArrayList<>();
        if (inRange.stream().anyMatch(d -> d.textDay.contains("雨") || d.textNight.contains("雨"))) {
            tips.add("有降雨可能，建议带伞");
        }
        if (lo <= 10) tips.add("气温偏低，注意保暖");
        if (hi >= 30) tips.add("天气较热，注意防晒补水");
        if (tips.isEmpty()) tips.add("天气条件温和，正常准备即可");

        return WeatherStepResult.builder()
                .city(request.getCity())
                .travelTime(request.getTravelTime())
                .forecastSummary(summary)
                .packingTips(tips)
                .daily(daily)
                .build();
    }

    /**
     * 调用和风天气的城市搜索 GeoAPI，把城市中文名转成 locationId。
     *
     * 其中这段链式调用拆开理解：
     *   qweatherWebClient.get()          → 构建一个 GET 请求
     *       .uri(b -> b.path(...)         → 设置请求路径
     *           .queryParam(...)          → 设置查询参数
     *           .build())
     *       .retrieve()                   → 发送请求
     *       .bodyToMono(XxxResponse.class) → 把响应 JSON 自动反序列化成 Java 对象
     *       .block()                      → 阻塞等待结果返回（因为我们在同步线程里调用）
     */
    private String lookupCityId(String city) {
        CityLookupResponse resp = qweatherWebClient.get()
                .uri(b -> b.path("/geo/v2/city/lookup")
                        .queryParam("location", city)
                        .queryParam("range", "cn")
                        .queryParam("number", 1)
                        .queryParam("lang", "zh")
                        .build())
                .retrieve()
                .bodyToMono(CityLookupResponse.class)
                .block();
        if (resp != null) {
            return resp.location.get(0).id;
        }

        // 若resp为空，默认返回北京的城市代码作为兜底
        return "101010100";
    }

    /**
     * 调用和风天气的逐日预报接口，获取未来 N 天的天气预报。
     *
     * URI 模板 "/v7/weather/{seg}?location={id}&lang=zh" 中：
     *   {seg} 会被 segment 替换，如 "7d"
     *   {id}  会被 locationId 替换，如 "101010100"
     * 最终请求类似：/v7/weather/7d?location=101010100&lang=zh
     *
     * 返回的 DailyForecastResponse 里的 daily 就是逐日预报列表。
     */
    private List<DailyForecast> fetchForecast(String locationId, String segment) {
        DailyForecastResponse resp = qweatherWebClient.get()
                .uri("/v7/weather/{seg}?location={id}&lang=zh", segment, locationId)
                .retrieve()
                .bodyToMono(DailyForecastResponse.class)
                .block();
        return resp.daily;
    }
}
