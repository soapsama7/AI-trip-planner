package com.aitripplannerbackend.service.impl;

import com.aitripplannerbackend.dto.TaskStatusResponse;
import com.aitripplannerbackend.dto.TripPlanResult;
import com.aitripplannerbackend.service.TaskProgressService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import static com.aitripplannerbackend.utils.contents.redisContents.PROGRESS_CACHE_PREFIX;
import static com.aitripplannerbackend.utils.contents.redisContents.RESULT_CACHE_PREFIX;

/**
 * 任务进度查询服务实现。
 *
 * 从 Redis 中读取任务的进度、结果、错误信息，组装成 TaskStatusResponse 返回。
 *
 * <h3>Redis 中的数据来源</h3>
 * TripPlannerServiceImpl 在执行任务时会：
 * - 每个步骤写入进度到 PROGRESS_CACHE_PREFIX + taskId
 * - 任务完成后写入结果到 RESULT_CACHE_PREFIX + taskId
 * - 任务失败后写入错误到 ERROR_CACHE_PREFIX + taskId
 *
 * 这个类就是去读这些 key。
 */
@Service
@RequiredArgsConstructor
public class TaskProgressServiceImpl implements TaskProgressService {

    private static final String ERROR_CACHE_PREFIX = "trip:task:error:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 查询任务状态。
     *
     * 分别从 Redis 读取进度、结果、错误三个 key：
     * - progress：数字字符串 "0"~"100"，-1 表示出错
     * - result：TripPlanResult 的 JSON（仅任务完成后有）
     * - error：错误信息字符串（仅任务失败后有）
     */
    @Override
    public TaskStatusResponse getTaskStatus(String taskId) {
        String progressValue = stringRedisTemplate.opsForValue().get(PROGRESS_CACHE_PREFIX + taskId);
        String resultValue = stringRedisTemplate.opsForValue().get(RESULT_CACHE_PREFIX + taskId);
        String errorValue = stringRedisTemplate.opsForValue().get(ERROR_CACHE_PREFIX + taskId);

        TaskStatusResponse response = new TaskStatusResponse();
        response.setTaskId(taskId);
        response.setProgress(parseProgress(progressValue));
        response.setError(errorValue);

        if (resultValue != null && !resultValue.isBlank()) {
            try {
                response.setResult(objectMapper.readValue(resultValue, TripPlanResult.class));
            } catch (Exception ignored) {
                response.setResult(null);
            }
        }
        return response;
    }

    /**
     * 安全地把字符串解析为整数。
     * Redis 存的是字符串（如 "75"），需要转成 Integer。
     * 如果为 null 或格式不对（比如被人手动改了 Redis 的值），返回 0 而不是抛异常。
     */
    private static Integer parseProgress(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
