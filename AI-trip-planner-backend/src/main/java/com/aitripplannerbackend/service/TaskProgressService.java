package com.aitripplannerbackend.service;

import com.aitripplannerbackend.dto.TaskStatusResponse;

/**
 * 任务进度查询服务接口。
 *
 * 提供根据 taskId 查询任务当前状态的能力。
 * 前端可以通过轮询 GET /api/trips/tasks/{taskId} 来调用这个服务，
 * 作为 SSE 实时推送的兜底方案（比如 SSE 连接断开后重连时用）。
 */
public interface TaskProgressService {

    /**
     * 从 Redis 中查询指定任务的进度、结果和错误信息。
     *
     * @param taskId 任务 ID
     * @return 包含 progress、result、error 的状态对象
     */
    TaskStatusResponse getTaskStatus(String taskId);
}
