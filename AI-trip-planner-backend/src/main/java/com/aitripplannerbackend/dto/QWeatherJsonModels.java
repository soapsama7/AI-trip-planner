package com.aitripplannerbackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 和风天气 API 返回的 JSON 数据模型。
 *
 * 和风天气的响应都是 JSON，字段非常多，但当前项目中只关心其中几个。
 * 加了 @JsonIgnoreProperties(ignoreUnknown = true) 后，
 * Jackson 在反序列化时会自动忽略没有定义的字段，不会报错。
 *
 * 这里用 public 字段而不是 getter/setter，是因为这些类只用来接收数据，不需要封装，写成 public 更简洁。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QWeatherJsonModels {

    /**
     * 城市搜索接口 /geo/v2/city/lookup 的响应。
     * 文档：https://dev.qweather.com/docs/api/geoapi/city-lookup
     *
     * 示例响应：
     * {
     *   "code": "200",
     *   "location": [
     *     { "id": "101010100", "name": "北京", "adm1": "北京市", "adm2": "北京" }
     *   ]
     * }
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CityLookupResponse {
        /** 状态码，"200" 表示成功 */
        public String code;
        /** 匹配到的城市列表，我们只取第一个 */
        public List<CityLocation> location;
    }

    /**
     * 城市搜索结果中的单个城市信息。
     * 我们主要用 id 来查天气，其他字段备用。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CityLocation {
        /** 城市 ID，查天气预报时要传这个（如 "101010100"） */
        public String id;
        /** 城市名（如 "北京"） */
        public String name;
        /** 一级行政区（如 "北京市"） */
        public String adm1;
        /** 二级行政区（如 "北京"） */
        public String adm2;
    }

    /**
     * 逐日天气预报接口 /v7/weather/{days} 的响应。
     * 文档：https://dev.qweather.com/docs/api/weather/weather-daily-forecast/
     *
     * 示例响应（因为只取 daily 数组，所以省略了很多字段）：
     * {
     *   "code": "200",
     *   "daily": [
     *     { "fxDate": "2026-04-05", "textDay": "晴", "textNight": "晴", "tempMax": "19", "tempMin": "5", ... }
     *   ]
     * }
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DailyForecastResponse {
        /** 状态码 */
        public String code;
        /** 逐日预报数组，每个元素代表一天 */
        public List<DailyForecast> daily;
    }

    /**
     * 单日天气预报。
     *
     * 和风 API 实际返回了 20 多个字段（风向、湿度、气压等），
     * 但该项目只用到了下面这几个，其余的被 @JsonIgnoreProperties 自动忽略。
     *
     * 注意：温度、降水量等字段在 JSON 里都是字符串（如 "19"），不是数字。
     * 所以这里先用 String 接收，在业务代码里再转 int。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DailyForecast {
        /** 预报日期，格式 "2026-04-05" */
        @JsonProperty("fxDate")
        public String fxDate;

        /** 白天天气状况（如 "晴"、"多云"、"小雨"） */
        @JsonProperty("textDay")
        public String textDay;

        /** 夜间天气状况 */
        @JsonProperty("textNight")
        public String textNight;

        /** 最高温度，字符串格式（如 "19"） */
        @JsonProperty("tempMax")
        public String tempMax;

        /** 最低温度 */
        @JsonProperty("tempMin")
        public String tempMin;

        /** 降水量（毫米），如 "0.0" */
        @JsonProperty("precip")
        public String precip;

        /** 紫外线强度指数，如 "6" */
        @JsonProperty("uvIndex")
        public String uvIndex;
    }
}
