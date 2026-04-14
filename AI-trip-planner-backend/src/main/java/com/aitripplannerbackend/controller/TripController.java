package com.aitripplannerbackend.controller;

import com.aitripplannerbackend.dto.Result;
import com.aitripplannerbackend.dto.TripGenerateRequest;
import com.aitripplannerbackend.dto.TripHistoryPageResponse;
import com.aitripplannerbackend.dto.TripPlanResult;
import com.aitripplannerbackend.dto.TripStreamEvent;
import com.aitripplannerbackend.dto.TaskStatusResponse;
import com.aitripplannerbackend.dto.TaskStartResponse;
import com.aitripplannerbackend.service.TaskProgressService;
import com.aitripplannerbackend.service.TripHistoryService;
import com.aitripplannerbackend.service.TripPlannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 旅行规划 REST Controller —— 整个后端的 HTTP 入口。
 *
 * <h3>所有接口一览</h3>
 * <table>
 *   <tr><th>方法</th><th>路径</th><th>功能</th></tr>
 *   <tr><td>POST</td><td>/api/trips/plan</td><td>启动规划任务，返回 taskId</td></tr>
 *   <tr><td>GET</td><td>/api/trips/tasks/{taskId}/events</td><td>SSE 订阅任务进度</td></tr>
 *   <tr><td>GET</td><td>/api/trips/history</td><td>查询历史记录</td></tr>
 *   <tr><td>GET</td><td>/api/trips/tasks/{taskId}</td><td>轮询查询任务状态</td></tr>
 * </table>
 *
 * <h3>注解说明</h3>
 * <ul>
 *   <li>{@code @RestController}：= @Controller + @ResponseBody，方法返回值自动序列化为 JSON</li>
 *   <li>{@code @RequestMapping("/api/trips")}：所有接口的公共路径前缀</li>
 *   <li>{@code @RequiredArgsConstructor}：Lombok 自动生成带 final 字段的构造函数，
 *       Spring 通过构造函数注入依赖（推荐的注入方式，比 @Autowired 更好）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
@Tag(name = "旅行规划", description = "异步规划任务、SSE、轮询、草稿/保存与历史记录")
public class TripController {

    private final TripPlannerService tripPlannerService;
    private final TripHistoryService tripHistoryService;
    private final TaskProgressService taskProgressService;

    /**
     * 启动异步规划任务。
     *
     * 前端提交请求后，后端立刻返回一个 taskId（不等任务完成），
     * 前端拿到 taskId 后通过 subscribeTaskEvents 建立 SSE 连接接收实时进度。
     *
     * {@code @Valid} 会触发 TripGenerateRequest 上的校验注解（@NotBlank、@NotNull 等），
     * 校验不通过自动返回 400 Bad Request。
     */
    @PostMapping("/plan")
    @Operation(summary = "启动旅行规划任务", description = "立即返回 taskId，不等待规划完成；前端用 taskId 订阅 SSE 或轮询状态。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "任务成功创建"),
            @ApiResponse(responseCode = "400", description = "请求体验证失败（城市、时间、预算等不符合校验）")
    })
    public Result<TaskStartResponse> startTask(@Valid @RequestBody TripGenerateRequest request) {
        String taskId = tripPlannerService.startTask(request);
        return Result.ok(TaskStartResponse.builder().taskId(taskId).build());
    }

    /**
     * SSE 实时事件流端点 —— 前端订阅任务进度。
     *
     * <h3>什么是 SSE（Server-Sent Events）？</h3>
     * SSE 是一种 HTTP 长连接技术：客户端发起请求后，服务端保持连接不断开，
     * 可以持续往客户端推送数据。和 WebSocket 不同，SSE 是单向的（服务端→客户端）。
     *
     * <h3>返回类型解读</h3>
     * {@code Flux<ServerSentEvent<TripStreamEvent>>}：
     * - Flux：响应式流，可以推送 0~N 个元素
     * - ServerSentEvent：SSE 的标准包装，包含 id、data 等字段
     * - TripStreamEvent：我们自定义的事件数据
     *
     * produces = TEXT_EVENT_STREAM_VALUE 告诉浏览器这是 SSE 流，
     * 浏览器/前端的 EventSource 就知道用 SSE 协议来解析。
     */
    @GetMapping(value = "/tasks/{taskId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅任务 SSE 事件流",
            description = """
                    Content-Type: text/event-stream。
                    每条事件的 data 为 JSON，结构见 TripStreamEvent（type=PROGRESS|DONE|ERROR）。
                    Swagger UI 对流式响应支持有限，建议用前端 EventSource 或 curl 调试。
                    """)
    @ApiResponses(@ApiResponse(responseCode = "200", description = "持续推送直至任务结束或连接关闭",
            content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                    schema = @Schema(implementation = TripStreamEvent.class))))
    public Flux<ServerSentEvent<TripStreamEvent>> subscribeTaskEvents(
            @Parameter(description = "启动任务接口返回的 taskId") @PathVariable String taskId) {
        return tripPlannerService.subscribeEvents(taskId);
    }

    /**
     * 主动查询任务状态（轮询兜底）。
     * 当 SSE 连接断开后，前端可以用这个接口轮询任务是否完成。
     */
    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "主动查询任务状态", description = "当SSE连接断开后，前端可以用这个接口轮询任务是否完成，若未完成则重新建立连接")
    @ApiResponses({
            @ApiResponse(responseCode = "200")
    })
    public Result<TaskStatusResponse> taskStatus(
            @Parameter(description = "任务 ID") @PathVariable String taskId) {
        return Result.ok(taskProgressService.getTaskStatus(taskId));
    }

    /**
     * 查询历史记录（分页）。
     *
     * {@code @RequestParam(defaultValue = "1")} 表示如果前端没传这个参数，默认值为 1。
     * 比如 GET /api/trips/history 等价于 GET /api/trips/history?page=1&size=10
     */
    @GetMapping("/history")
    @Operation(summary = "分页查询历史行程", description = "页码从 1 开始；未传参数时默认 page=1、size=5。")
    @ApiResponses(@ApiResponse(responseCode = "200"))
    public Result<TripHistoryPageResponse> history(
            @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "5") int size) {
        return Result.ok(tripHistoryService.listHistory(page, size));
    }


    /**
     * 取消任务。
     * 这里只是标记取消，运行中的步骤会在关键节点检查并尽快退出。
     */
    @PostMapping("/tasks/{taskId}/cancel")
    @Operation(summary = "取消任务", description = "标记取消，运行中的步骤会在检查点退出。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "后端已受理取消请求"))
    public Result<String> cancelTask(@PathVariable String taskId) {
        tripPlannerService.cancelTask(taskId);
        return Result.ok("取消请求已受理");
    }

    /**
     * 保存用户在编辑模式下的草稿（仅覆盖 Redis 结果，不入库）。
     */
    @PostMapping("/tasks/{taskId}/draft")
    @Operation(summary = "保存草稿", description = "编辑模式下覆盖 Redis 中的结果预览，不入库。待用户保存后再入库")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "保存成功"))
    public Result<String> saveDraft(
            @Parameter(description = "任务 ID") @PathVariable String taskId,
            @RequestBody TripPlanResult planResult) {
        tripPlannerService.saveDraft(taskId, planResult);
        return Result.ok("草稿保存成功");
    }

    /**
     * 用户确认后正式保存行程到数据库。
     * 保存后该 taskId 的行程不允许再编辑。
     */
    @PostMapping("/tasks/{taskId}/save")
    @Operation(summary = "正式保存行程", description = "写入数据库；首次保存后该 taskId 对应行程不可再编辑。")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "保存结果说明"))
    public Result<String> saveTrip(@PathVariable String taskId) {
        boolean firstSave = tripPlannerService.saveTrip(taskId);
        if (firstSave) {
            return Result.ok("行程已保存，后续不可修改");
        }
        return Result.fail(400,"该行程已保存过，无需重复保存");
    }
}
