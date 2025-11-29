# 课程选择微服务项目 (Course Cloud)

## 1. 项目简介

### 1.1 项目概述
Course Cloud 是基于单体校园选课系统拆分的微服务架构项目，将原应用拆分为**用户服务、课程目录服务、选课服务**三个独立微服务，实现服务解耦、独立部署和数据隔离。每个服务拥有专属数据库，选课服务从原有的 `RestTemplate` 方式改为**OpenFeign**实现跨服务声明式通信，并集成 Resilience4j 实现熔断降级容错机制，同时支持多实例部署与客户端负载均衡，进一步提升系统的可用性、可扩展性与维护性。

### 1.2 微服务架构说明
| 服务名称 | 基础端口 | 核心功能 | 数据库 | 部署实例数 |
|----------|----------|----------|--------|------------|
| **user-service**（用户服务） | 8081 | 学生信息增删改查，提供学生数据接口 | user_db (3306) | 3个 |
| **catalog-service**（课程目录服务） | 8082 | 课程信息增删改查（含讲师/排课嵌套对象），课程容量校验 | catalog_db (3307) | 3个 |
| **enrollment-service**（选课服务） | 8083 | 选课/退课管理，调用用户服务验证学生、调用课程服务验证课程及容量 | enrollment_db (3308) | 1个 |

#### 服务间通信与容错特性
- **通信方式**: 声明式HTTP调用（基于 **OpenFeign**），替代原 `RestTemplate` 方式
- **依赖关系**: enrollment-service → user-service + catalog-service
- **负载均衡**: 基于Nacos的客户端负载均衡，自动分发请求至多实例
- **容错机制**: Resilience4j熔断器（失败率阈值50%，滑动窗口10次）+ Fallback降级处理
- **数据隔离**: 各服务独立数据库，禁止跨服务直接访问数据库
- **核心交互**: 选课操作需验证学生存在性、课程存在性及容量，退课操作需同步更新课程选课人数

---

## 2. 架构图

```
┌─────────────┐
│   客户端    │
└──────┬──────┘
       │
       │  负载均衡请求分发
       │  (OpenFeign + Nacos)
       ▼
┌─────────────────────┐
│ enrollment-service  │ (8083, 1实例)
│ - 选课/退课管理     │
│ - OpenFeign调用     │
│ - 熔断降级处理      │
└─────────┬───────────┘
          │
          ├─────────────────────────────────┬─────────────────────────────────┐
          │                                 │                                 │
          ▼                                 ▼                                 ▼
┌─────────────────────┐     ┌─────────────────────┐     ┌─────────────────────┐
│ user-service-1      │     │ user-service-2      │     │ user-service-3      │
│ (8081)              │     │ (8081)              │     │ (8081)              │
└─────────┬───────────┘     └─────────┬───────────┘     └─────────┬───────────┘
          │                           │                           │
          └───────────────────────────┼───────────────────────────┘
                                      ▼
                             ┌─────────────┐
                             │ user_db     │
                             │ (3306)      │
                             └─────────────┘
          │
          ├─────────────────────────────────┬─────────────────────────────────┐
          │                                 │                                 │
          ▼                                 ▼                                 ▼
┌─────────────────────┐     ┌─────────────────────┐     ┌─────────────────────┐
│ catalog-service-1   │     │ catalog-service-2   │     │ catalog-service-3   │
│ (8082)              │     │ (8082)              │     │ (8082)              │
└─────────┬───────────┘     └─────────┬───────────┘     └─────────┬───────────┘
          │                           │                           │
          └───────────────────────────┼───────────────────────────┘
                                      ▼
                             ┌─────────────┐
                             │ catalog_db  │
                             │ (3307)      │
                             └─────────────┘
          │
          ▼
┌─────────────┐
│ enrollment_ │
│ db (3308)   │
└─────────────┘
┌─────────────┐
│ Nacos       │ (服务注册/发现)
│ (8848/8849) │
└─────────────┘
```

---

## 3. 服务发现与Nacos

为了实现服务的动态发现、负载均衡和统一配置管理，项目引入了 **Nacos** 作为服务注册与发现中心，并基于Nacos实现OpenFeign的客户端负载均衡。

### 3.1 Nacos 部署

Nacos 通过 `docker-compose.yml` 文件与业务服务一同部署。配置要点如下：

```yaml
# docker-compose.yml (nacos服务部分)
nacos:
  image: nacos/nacos-server:v3.1.0
  container_name: nacos-standalone
  environment:
    - MODE=standalone
    - NACOS_AUTH_IDENTITY_KEY=serverIdentity
    - NACOS_AUTH_IDENTITY_VALUE=serverIdentity
    - NACOS_AUTH_ENABLE=true
    - JAVA_OPT=-Xms256m -Xmx256m
    - NACOS_AUTH_TOKEN=VGhpc0lzTXlTdXBlckxvbmdBbmRBYnNvbHV0ZWx5U2VjdXJlU2VjcmV0S2V5Rm9yTmFjb3NBdXRoMTIzNDU=
  ports:
    - "8848:8848" # API Port
    - "8849:8080" # UI Port for Nacos 3.x
    - "9848:9848" # Service Discovery Port
  networks:
    - course-network
  healthcheck:
    test: ["CMD-SHELL", "curl -f http://localhost:8080/ || exit 1"]
    interval: 15s
    timeout: 10s
    retries: 10
    start_period: 120s
```

- **Nacos 控制台访问地址**: `http://localhost:8849`
- **服务注册地址 (供微服务使用)**: `nacos:8848`

### 3.2 微服务接入Nacos配置

为了让每个微服务能够向 Nacos 注册自己，并发现其他服务，需要完成以下两步配置：

#### 1. 添加Maven依赖

在每个微服务（`user-service`, `catalog-service`, `enrollment-service`）的 `pom.xml` 文件中，必须添加以下两个核心依赖：

```xml
<!-- Nacos Service Discovery Starter -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>

<!-- Spring Boot Actuator (用于健康检查) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

#### 2. 修改主启动类

在每个微服务的主启动类上，添加 `@EnableDiscoveryClient` 注解开启服务发现功能；**enrollment-service** 需额外添加 `@EnableFeignClients` 注解启用OpenFeign客户端。

```java
// enrollment-service 主启动类示例
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients // 启用OpenFeign客户端
public class EnrollmentServiceApplication {
    // ...
}
```

#### 3. 修改配置文件

由于所有服务在Docker环境中都只加载 `application-prod.yml`，需在该文件中指定 Nacos 服务器地址。以 `user-service` 为例：

```yaml
# user-service/src/main/resources/application-prod.yml
spring:
  application:
    name: user-service # 服务注册到Nacos时使用的服务名
  cloud:
    nacos:
      discovery:
        server-addr: nacos:8848
        ephemeral: true
        heart-beat-interval: 5000
        heart-beat-timeout: 15000
        username: nacos
        password: nacos
        access-token: VGhpc0lzTXlTdXBlckxvbmdBbmRBYnNvbHV0ZWx5U2VjdXJlU2VjcmV0S2V5Rm9yTmFjb3NBdXRoMTIzNDU=

# 暴露健康检查端点，以便Nacos监控服务状态
management:
  endpoints:
    web:
      exposure:
        include: '*'
  endpoint:
    health:
      show-details: always
```

---

## 4. 服务间通信与负载均衡

### 4.1 OpenFeign 集成
enrollment-service 已替换原 `RestTemplate` 为 **OpenFeign** 实现声明式服务调用，通过定义Feign Client接口简化跨服务通信，核心配置如下：

#### 1. 添加Maven依赖
在 `enrollment-service/pom.xml` 中添加OpenFeign与Resilience4j依赖：
```xml
<!-- OpenFeign -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
<!-- Resilience4j 熔断器 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

#### 2. 定义Feign Client接口
在enrollment-service中创建`UserClient`和`CatalogClient`，以`UserClient`为例：
```java
@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserClient {
    @GetMapping("/api/users/students/{id}")
    StudentDto getStudent(@PathVariable Long id);
}
```

#### 3. 配置文件
在`enrollment-service/application.yml`中配置OpenFeign与熔断器参数：
```yaml
# OpenFeign 配置
feign:
  client:
    config:
      default:
        connectTimeout: 3000 # 连接超时3秒
        readTimeout: 5000    # 读取超时5秒
  circuitbreaker:
    enabled: true # 启用熔断器

# Resilience4j 熔断器配置
resilience4j:
  circuitbreaker:
    instances:
      user-service:
        failureRateThreshold: 50 # 失败率阈值50%
        slidingWindowSize: 10    # 滑动窗口10次
      catalog-service:
        failureRateThreshold: 50
        slidingWindowSize: 10
```

### 4.2 Fallback 降级处理
为每个Feign Client实现降级处理类，服务不可用时触发容错逻辑，以`UserClientFallback`为例：
```java
@Slf4j
@Component
public class UserClientFallback implements UserClient {
    @Override
    public StudentDto getStudent(Long id) {
        log.warn("UserClient fallback triggered for student: {}", id);
        throw new ServiceUnavailableException("用户服务暂时不可用，请稍后再试");
    }
}
```

### 4.3 多实例部署
通过Docker Compose配置，实现`user-service`和`catalog-service`各3个实例的部署，关键配置要点：
- 每个实例使用唯一`container_name`（如`user-service-1`/`user-service-2`/`user-service-3`）
- 所有实例使用相同镜像和端口，通过Docker网络实现通信
- 所有服务注册至同一Nacos节点，支持负载均衡

### 4.4 负载均衡验证
1. 在各服务Controller中注入端口号并打印日志，标识处理请求的实例：
   ```java
   @Value("${server.port}")
   private String port;

   @GetMapping("/api/students/{id}")
   public StudentDto getStudent(@PathVariable Long id) {
       log.info("处理请求的实例端口: {}", port);
       // 业务逻辑...
   }
   ```
2. 连续发送选课请求，通过日志观察请求被分发至不同实例。

### 4.5 熔断降级测试
1. 停止所有`user-service`实例：`docker compose stop user-service-1 user-service-2 user-service-3`
2. 发送选课请求，观察日志中Fallback逻辑触发
3. 重启服务：`docker compose start user-service-1 user-service-2 user-service-3`，验证服务恢复正常

---

## 5. 技术栈

### 5.1 核心技术栈
| 类别 | 技术选型 | 版本要求 |
|------|----------|----------|
| 后端框架 | Spring Boot | 3.2.6 |
| 编程语言 | Java | 21 (JDK 21) |
| 数据持久化 | Spring Data JPA + Hibernate | 随Spring Boot版本 |
| 数据库 | MySQL | 8.4 |
| 连接池 | HikariCP | 随Spring Boot版本 |
| 容器化 | Docker + Docker Compose | Docker 20.10+ / Compose 2.0+ |
| 构建工具 | Maven | 3.8+ |
| 服务注册与发现 | Nacos | v3.1.0 |
| 服务间通信 | Spring Cloud OpenFeign | 随Spring Cloud版本 |
| 容错机制 | Resilience4j CircuitBreaker | 随Spring Cloud版本 |
| 负载均衡 | Spring Cloud LoadBalancer | 随Spring Cloud版本 |

### 5.2 辅助依赖
- **Lombok**: 1.18.32（简化实体类代码）
- **SpringDoc OpenAPI**: 2.3.0（自动生成API文档）
- **Jakarta Validation**: 参数校验
- **curl/jq**: 接口测试与JSON解析（可选）

---

## 6. 环境要求

### 6.1 必需软件
- JDK 21 或更高版本
- Maven 3.8+
- Docker 20.10+
- Docker Compose 2.0+（使用 `docker compose` 命令，非 `docker-compose`）

### 6.2 推荐配置
- 内存：至少 4GB RAM（多实例部署建议8GB+）
- 磁盘：至少 2GB 可用空间
- 操作系统：Linux / macOS / Windows (需安装 WSL2)

### 6.3 环境验证
```bash
# 检查 Java 版本（需显示 21+）
java -version

# 检查 Maven 版本（需显示 3.8+）
mvn -version

# 检查 Docker 版本（需显示 20.10+）
docker --version

# 检查 Docker Compose 版本（需显示 2.0+）
docker compose version
```

---

## 7. 构建和运行步骤

### 7.1 克隆项目
```bash
git clone <repository-url>
cd course-cloud
```

### 7.2 构建项目
在项目根目录执行 Maven 打包命令（跳过测试以加快构建）：
```bash
# 清理并打包所有服务
mvn clean package -DskipTests
```

**打包结果**：
- user-service: `user-service/target/user-service.jar`
- catalog-service: `catalog-service/target/catalog-service.jar`
- enrollment-service: `enrollment-service/target/enrollment-service.jar`

### 7.3 启动多实例服务
使用 Docker Compose 一键启动所有服务（含Nacos+3个数据库+多实例微服务）：
```bash
# 构建镜像并后台启动所有服务（含多实例配置）
docker compose up -d --build

# 查看服务运行状态（需Nacos+3个数据库+1个enrollment+3个user+3个catalog，共11个服务）
docker compose ps

# 实时查看所有服务日志
docker compose logs -f
```

### 7.4 等待服务启动完成
服务启动需 60-90 秒（Nacos初始化+多实例注册+数据库初始化），可通过日志验证：
```bash
# 验证 Nacos 启动成功
docker compose logs nacos | grep "Nacos started successfully"

# 验证 user-service 多实例启动成功
docker compose logs user-service-1 | grep "Started UserServiceApplication"

# 验证 enrollment-service 启动成功
docker compose logs enrollment-service | grep "Started EnrollmentServiceApplication"
```

### 7.5 服务可用性验证
```bash
# 测试 user-service（应返回学生列表，初始为空数组）
curl http://localhost:8081/api/students

# 测试 catalog-service（应返回课程列表，初始为空数组）
curl http://localhost:8082/api/courses

# 测试 enrollment-service（应返回选课记录列表，初始为空数组）
curl http://localhost:8083/api/enrollments
```

### 7.6 负载均衡与熔断测试
```bash
# 1. 负载均衡测试：连续发送选课请求，观察多实例日志
for i in {1..10}; do curl -X POST "http://localhost:8083/api/enrollments?studentId=2024001&courseCode=CS101"; done

# 2. 熔断降级测试：停止所有user-service实例
docker compose stop user-service-1 user-service-2 user-service-3

# 3. 发送请求触发Fallback
curl http://localhost:8083/api/enrollments?studentId=2024001&courseCode=CS101

# 4. 重启user-service实例，验证服务恢复
docker compose start user-service-1 user-service-2 user-service-3
```

### 7.7 停止服务
```bash
# 停止所有服务（保留数据卷，数据库数据不丢失）
docker compose down

# 停止并删除数据卷（清空数据库，适用于重新测试）
docker compose down -v
```

---

## 8. API 文档

### 8.1 在线文档访问
| 服务名称 | 访问地址 | 说明 |
|----------|----------|------|
| user-service | http://localhost:8081/swagger-ui.html | 学生管理API文档 |
| catalog-service | http://localhost:8082/swagger-ui.html | 课程管理API文档 |
| enrollment-service | http://localhost:8083/swagger-ui.html | 选课管理API文档 |

### 8.2 User Service (基础端口: 8081)
#### 学生管理接口
| 方法 | 路径 | 描述 | 请求体 | 响应 |
|------|------|------|--------|------|
| GET | `/api/students` | 获取所有学生 | - | 学生列表（JSON数组） |
| GET | `/api/students/{studentId}` | 按学号查询学生 | - | 学生详情（JSON对象） |
| POST | `/api/students` | 创建学生 | Student JSON | 新建学生详情 |
| PUT | `/api/students/{studentId}` | 更新学生 | Student JSON | 更新后学生详情 |
| DELETE | `/api/students/{studentId}` | 删除学生 | - | 204 No Content |

#### Student 数据结构
```json
{
  "id": "uuid",
  "studentId": "2024001",
  "name": "张三",
  "email": "zhangsan@example.com",
  "major": "计算机科学",
  "grade": 2024
}
```

#### 接口示例
```bash
# 创建学生
curl -X POST http://localhost:8081/api/students \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "2024001",
    "name": "张三",
    "email": "zhangsan@example.com",
    "major": "计算机科学",
    "grade": 2024
  }'

# 按学号查询学生
curl http://localhost:8081/api/students/2024001
```

### 8.3 Catalog Service (基础端口: 8082)
#### 课程管理接口
| 方法 | 路径 | 描述 | 请求体 | 响应 |
|------|------|------|--------|------|
| GET | `/api/courses` | 获取所有课程 | - | 课程列表（JSON数组） |
| GET | `/api/courses/{courseCode}` | 按课程代码查询课程 | - | 课程详情（JSON对象） |
| POST | `/api/courses` | 创建课程（含讲师/排课信息） | Course JSON | 新建课程详情 |
| PUT | `/api/courses/{courseCode}` | 更新课程 | Course JSON | 更新后课程详情 |
| DELETE | `/api/courses/{courseCode}` | 删除课程 | - | 204 No Content |
| GET | `/api/courses/{courseCode}/capacity` | 检查课程剩余容量 | - | 剩余容量（数字） |

#### Course 数据结构（含嵌套对象）
```json
{
  "id": "uuid",
  "courseName": "数据结构",
  "courseCode": "CS101",
  "credits": 4,
  "maxStudents": 100,
  "enrolled": 0,
  "instructor": {
    "name": "李教授",
    "email": "li@example.com",
    "department": "计算机科学系"
  },
  "scheduleSlot": {
    "dayOfWeek": "MONDAY",
    "startTime": "08:00",
    "endTime": "10:00",
    "location": "教学楼A101"
  }
}
```

#### 接口示例
```bash
# 创建课程（含讲师/排课嵌套信息）
curl -X POST http://localhost:8082/api/courses \
  -H "Content-Type: application/json" \
  -d '{
    "courseName": "数据结构",
    "courseCode": "CS101",
    "credits": 4,
    "maxStudents": 100,
    "instructor": {
      "name": "李教授",
      "email": "li@example.com",
      "department": "计算机科学系"
    },
    "scheduleSlot": {
      "dayOfWeek": "MONDAY",
      "startTime": "08:00",
      "endTime": "10:00",
      "location": "教学楼A101"
    }
  }'

# 按课程代码查询课程
curl http://localhost:8082/api/courses/CS101
```

### 8.4 Enrollment Service (端口: 8083)
#### 选课管理接口
| 方法 | 路径 | 描述 | 请求参数/体 | 响应 |
|------|------|------|-------------|------|
| POST | `/api/enrollments` | 学生选课 | `studentId`（学号）、`courseCode`（课程代码） | 选课记录详情 |
| DELETE | `/api/enrollments/{studentId}/{courseCode}` | 学生退课 | - | 204 No Content |
| GET | `/api/enrollments/student/{studentId}` | 按学生查询选课记录 | - | 选课记录列表 |
| GET | `/api/enrollments/course/{courseCode}` | 按课程查询选课记录 | - | 选课记录列表 |
| GET | `/api/enrollments/check/{studentId}/{courseCode}` | 检查学生是否已选该课程 | - | 布尔值（true/false） |

#### Enrollment 数据结构
```json
{
  "id": "uuid",
  "studentId": "2024001",
  "courseCode": "CS101",
  "status": "ACTIVE",
  "enrolledAt": "2024-11-27T10:30:00"
}
```

#### 接口示例
```bash
# 学生选课（需先创建学生和课程）
curl -X POST "http://localhost:8083/api/enrollments?studentId=2024001&courseCode=CS101"

# 按学生查询选课记录
curl http://localhost:8083/api/enrollments/student/2024001

# 学生退课
curl -X DELETE http://localhost:8083/api/enrollments/2024001/CS101
```

---

## 9. 项目结构

```
course-cloud/
├── services/
│   ├── enrollment-service/       # 选课服务
│   │   ├── src/main/java/com/zjgsu/coursecloud/enrollment/
│   │   │   ├── client/           # Feign Client接口及降级类
│   │   │   │   ├── UserClient.java
│   │   │   │   ├── UserClientFallback.java
│   │   │   │   ├── CatalogClient.java
│   │   │   │   └── CatalogClientFallback.java
│   │   │   ├── controller/       # 接口层（选课相关API）
│   │   │   ├── service/          # 业务逻辑层（含OpenFeign调用）
│   │   │   ├── repository/       # 数据访问层（JPA）
│   │   │   ├── model/            # 实体类（Enrollment）
│   │   │   ├── dto/              # 数据传输对象（StudentDto/CourseDto）
│   │   │   ├── common/           # 异常处理
│   │   │   └── EnrollmentServiceApplication.java  # 应用入口
│   │   │── src/main/resources/
│   │   │   ├── application.yml   # 配置文件（OpenFeign/熔断器配置）
│   │   │   └── application-prod.yml
│   │   ├── Dockerfile            # 容器构建文件
│   │   └── pom.xml               # 服务依赖配置（OpenFeign/Resilience4j）
│   │
│   ├── user-service/             # 用户服务（3实例部署）
│   │   ├── src/main/java/com/zjgsu/coursecloud/user/
│   │   ├── src/main/resources/
│   │   ├── Dockerfile
│   │   └── pom.xml
│   │
│   ├── catalog-service/          # 课程目录服务（3实例部署）
│   │   ├── src/main/java/com/zjgsu/coursecloud/catalog/
│   │   ├── src/main/resources/
│   │   ├── Dockerfile
│   │   └── pom.xml
│
├── docker-compose.yml            # 多服务编排配置（Nacos+多实例微服务）
│---test-balance.sh      # 负载均衡测试脚本
├── test-services.sh              # 自动化测试脚本
└── README.md                     # 项目文档
```

---
