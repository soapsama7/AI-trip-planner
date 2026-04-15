# AI Trip Planner（后端）

基于 **Spring Boot 3** 与 **Spring WebFlux** 的异步旅行规划服务：调用LLM大模型与第三方数据服务生成行程，通过 **SSE** 推送任务进度，使用 **MySQL** 持久化、**Redis** 缓存任务状态与草稿和Swagger生成接口文档。

> 本说明仅覆盖 **后端** 技术栈与运行方式。仓库内另有前端子工程，此处不展开其技术选型。

---

## 技术栈


| 类别        | 技术                                                                  |
| --------- | ------------------------------------------------------------------- |
| JDK版本     | Java **17**                                                         |
| 框架        | Spring Boot **3.5.x**，Spring WebFlux（响应式 Web）                       |
| 持久化       | **MyBatis** 3.x、**MySQL** 8（`mysql-connector-j`）                    |
| 缓存 / 任务状态 | **Spring Data Redis**                                               |
| 校验        | Jakarta Bean Validation（`spring-boot-starter-validation`）           |
| JSON      | Jackson（含 **jsr310** 时间类型）                                          |
| AI        | **Spring AI**（`spring-ai-starter-model-deepseek`，兼容 OpenAI API 的端点） |
| 接口文档      | **springdoc-openapi** + **Swagger UI**（OpenAPI 3）                   |
| 构建        | **Maven**                                                           |


---

## 外部依赖说明

后端在规划流程中会调用：

- **大模型**：通过 `spring.ai.deepseek` 配置（默认示例指向火山方舟兼容的 OpenAI 风格 API，可按环境替换 `base-url` / `model`）。
- **和风天气（QWeather）**：`qweather.`* 配置，用于天气相关步骤。
- **高德地图**：`amap.`* 配置，用于地理相关能力（如展示景点位置，搜索酒店等）。

---

## 环境要求

- **JDK 17+**
- **Maven 3.6+**
- **MySQL 8**（本地或远程实例）
- **Redis**（默认无密码可连 `localhost:6379`）

---

## 数据库初始化

在 MySQL 中执行后端提供的建库建表脚本（与 `application.yml` 中的库名一致）：

```text
AI-trip-planner-backend/src/main/resources/sql/schema.sql
```

默认连接：`jdbc:mysql://localhost:3306/ai_trip_planner`（可通过环境变量覆盖，见下表）。

---

## 配置与环境变量

主要配置位于 `AI-trip-planner-backend/src/main/resources/application.yml`。常用环境变量如下（未设置时使用 `application.yml` 中的默认值）：


| 变量                                                            | 说明             |
| ------------------------------------------------------------- | -------------- |
| `MYSQL_USERNAME` / `MYSQL_PASSWORD`                           | MySQL 用户名 / 密码 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` / `REDIS_DB`   | Redis 连接       |
| `DEEPSEEK_API_KEY` / `DEEPSEEK_BASE_URL` / `DEEPSEEK_MODEL` 等 | 大模型调用相关参数      |
| `QWEATHER_API_KEY` / `QWEATHER_API_HOST`                      | 和风天气相关参数       |
| `AMAP_BASE_URL` / `AMAP_WEB_KEY`                              | 高德 Web 服务 Key  |


HTTP 服务默认端口：**8080**（`server.port`）

前端的.env文件中也有相应参数配置，主要为高德地图服务Key

---

## 构建与运行

```bash
cd AI-trip-planner-backend
mvn -DskipTests package
java -jar target/AI-trip-planner-backend-0.0.1-SNAPSHOT.jar
```

开发时也可直接：

```bash
cd AI-trip-planner-backend
mvn spring-boot:run
```

或导入IDEA中运行

启动成功后，HTTP 接口默认监听 `http://localhost:8080`。

---

## HTTP API 概览

统一前缀：`/api/trips`


| 方法     | 路径                       | 说明                                        |
| ------ | ------------------------ | ----------------------------------------- |
| `POST` | `/plan`                  | 提交城市、出行时间、预算，启动异步规划，返回 `taskId`           |
| `GET`  | `/tasks/{taskId}/events` | **前端SSE**（`text/event-stream`）订阅任务进度与完成事件 |
| `GET`  | `/tasks/{taskId}`        | 轮询任务状态（进度、错误、结果）                          |
| `POST` | `/tasks/{taskId}/cancel` | 标记取消任务                                    |
| `POST` | `/tasks/{taskId}/draft`  | 保存编辑草稿（只保存进Redis，不入库）                     |
| `POST` | `/tasks/{taskId}/save`   | 确认保存后写入数据库并锁定数据，不可再修改                     |
| `GET`  | `/history`               | 分页查询历史行程（`page`、`size` 查询参数）              |


---

## OpenAPI / Swagger UI

集成 **springdoc-openapi** 后，可在浏览器访问（具体路径以 springdoc 默认行为为准）：

- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

---

## 工程结构（后端）

```text
AI-trip-planner-backend/
├── pom.xml
└── src/main/java/com/aitripplannerbackend/
    ├── AiTripPlannerBackendApplication.java   # 启动类
    ├── controller/                            # HTTP 入口
    ├── service/                               # 业务逻辑与 Agent 编排相关
    ├── mapper/                                # MyBatis Mapper
    ├── entity/                                # 与表结构对应实体
    ├── dto/                                   # 请求/响应与 SSE 载荷
    └── config/                                # Redis、CORS、OpenAPI 等配置
    └── utils/                                 # 工具类和用到的常量
└── src/main/resources/
    ├── application.yml
    └── sql/schema.sql
```



---

## 我这个项目到底做了什么

顺着后端调用链的一点分析

- 现在这个项目仅是本地个人部署，因此没有考虑高并发和一些Web安全问题。但已经可以跑通完整链路

- 集成了`springdoc-openapi`，在springboot中自动扫描Controller和注解生成 OpenAPI 规范文档，再通过 Swagger UI 进行可视化展示和在线调试
- `startTask`会先校验预算（每日预算必须大于等于150元），然后根据随机UUID生成taskId，再将原始的请求表单存入redis（key为taskId），最后把真正请求的任务丢到“线程池”里异步执行（下面称为异步线程）。当前线程不会被阻塞，立刻返回taskId给前端
- 事件流基于Redis Pub/Sub构建，前端收到taskId之后，携带这一taskId立刻发送"/tasks/{taskId}/events"的SSE请求给后端，后端在该接口中订阅对应的Redis发布频道。将任务执行过程中发布的阶段性事件转发为 SSE 流推送给前端，从而实现实时进度展示。异步线程每处理完一个进度就往Redis的频道里面发布阶段性事件，SSE 接口作为消费者进行订阅和转发，从而实现任务执行与前端展示的解耦。前端断线重连后会重新发起 SSE 请求，后端根据 taskId 查询当前任务状态，如果任务已经完成则直接返回结果，否则继续推送后续进度（但可能会损失部分中间消息）

Redis Pub/Sub 是实时的，不具备持久化能力，如果订阅者断开连接，中间的消息可能会丢失。所以我这里是通过任务状态存储（Redis/数据库）来保证最终结果一致性。

- 异步线程先调用和风天气API查询当地用户所选择时段的天气，然后将天气结果传给LLM，LLM再推荐相关景点，最终再由LLM生成总的旅行规划建议，由于各个环节（除获取天气外）都需要依赖上一环节的结果，因此这里必须使用阻塞式编程
- 全部任务正确执行后，将Redis中的进度设为100，同时将生成的旅行规划保存进Redis（先不入库），供给后面的修改方案使用。若中间调用API抛了异常也有外部catch可以捕获
- 前端在启动异步任务后，会把后端返回的 taskId 持久化到浏览器 localStorage。这样即使页面刷新、关闭或者 SSE 断连，前端仍然能拿着这个 taskId 去调用状态查询接口做恢复。页面重新进入时，前端会先查询任务状态；如果发现任务已完成或失败，就直接结束恢复流程；如果任务仍在执行，才重新建立 SSE 连接继续接收实时进度。这里 taskId 本身在前端没有自动过期时间，但真正能否恢复，取决于后端 Redis 中任务状态缓存是否还在。我们项目里这个恢复窗口是 30 分钟。
- 最终生成的行程规划并不会直接入库，而是先保存到Redis，可供用户手动编辑并保存草稿。只有当用户点击“保存行程”的时候才会真正入库，并且之后无法再次修改
- 当前程序里采用一个简单的全局异常处理器`GlobalExceptionHandler`作为异常处理，提取异常消息和状态码反馈给前端。但并没有记录日志、细分异常等。因为感觉目前项目也不算很大，排查起来很方便而且也没有其它的错误信息，为了方便就先这样吧。后续如果要扩展的话可以再改

整个项目里面存了好多Key到Redis里面，这里做个总结：

| Redis Key              | 作用                                                         |
| :--------------------- | ------------------------------------------------------------ |
| EVENTS_CHANNEL_PREFIX  | Redis Pub/Sub 频道 Key                                       |
| ERROR_CACHE_PREFIX     | 存储错误信息的 Redis Key，只有任务中止时才有（如取消、出错） |
| CANCELLED_CACHE_PREFIX | 存储任务取消标记的 Redis Key（1 表示用户已取消）             |
| REQUEST_CACHE_PREFIX   | 存储原始请求 JSON（或用户编辑行程后覆盖的JSON）的Redis Key   |
| SAVED_CACHE_PREFIX     | 存储是否已正式保存到数据库的Redis Key（1 表示已保存，不可再编辑） |
| RESULT_CACHE_PREFIX    | 存储最终旅行计划结果（JSON 字符串）的Redis Key               |
| PROGRESS_CACHE_PREFIX  | 存储任务进度（0~100 的数字字符串）的Redis Key，-1 表示出错   |

