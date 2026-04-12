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

生产环境请通过**环境变量**注入密钥与地址，勿将真实密钥直接放在配置文件中或提交到仓库。

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

