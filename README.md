# 课程选择微服务项目 (Course Cloud)


## 1. 项目简介

### 1.1 项目概述
Course Cloud 是基于单体校园选课系统拆分的微服务架构项目，将原应用拆分为**用户服务、课程目录服务、选课服务**三个独立微服务，实现服务解耦、独立部署和数据隔离。每个服务拥有专属数据库，选课服务通过HTTP调用实现跨服务通信验证，提升系统可扩展性与维护性。

### 1.2 微服务架构说明
| 服务名称 | 端口 | 核心功能 | 数据库 |
|----------|------|----------|--------|
| **user-service**（用户服务） | 8081 | 学生信息增删改查，提供学生数据接口 | user_db (3306) |
| **catalog-service**（课程目录服务） | 8082 | 课程信息增删改查（含讲师/排课嵌套对象），课程容量校验 | catalog_db (3307) |
| **enrollment-service**（选课服务） | 8083 | 选课/退课管理，调用用户服务验证学生、调用课程服务验证课程及容量 | enrollment_db (3308) |

#### 服务间通信特性
- **通信方式**: RESTful HTTP（基于 RestTemplate）
- **依赖关系**: enrollment-service → user-service + catalog-service
- **数据隔离**: 各服务独立数据库，禁止跨服务直接访问数据库
- **核心交互**: 选课操作需验证学生存在性、课程存在性及容量，退课操作需同步更新课程选课人数

---

## 2. 架构图

```
┌─────────────┐
│   客户端    │
└──────┬──────┘
       │
       ├───────────────┐      ┌─────────────┐
       │               ▼      │             │
       │  ┌─────────────────────┐     ┌─────────────┐
       │  │ user-service        │────▶│ user_db     │
       │  │ (8081)              │     │ (3306)      │
       │  │ - 学生CRUD          │     └─────────────┘
       │  └─────────────────────┘
       │
       ├───────────────┐
       │               ▼
       │  ┌─────────────────────┐     ┌─────────────┐
       │  │ catalog-service     │────▶│ catalog_db  │
       │  │ (8082)              │     │ (3307)      │
       │  │ - 课程CRUD          │     └─────────────┘
       │  │ - 讲师/排课嵌套处理 │
       │  │ - 课程容量校验      │
       │  └─────────────────────┘
       │
       └───────────────┐
                       ▼
              ┌─────────────────────┐     ┌─────────────┐
              │ enrollment-service  │────▶│ enrollment_ │
              │ (8083)              │     │ db (3308)   │
              │ - 选课/退课管理     │     └─────────────┘
              │ - 学生验证(HTTP调用)│
              │ - 课程验证(HTTP调用)│
              │ - 重复选课校验      │
              └─────────────────────┘
```

---

## 3. 技术栈

### 3.1 核心技术栈
| 类别 | 技术选型 | 版本要求 |
|------|----------|----------|
| 后端框架 | Spring Boot | 3.2.6 |
| 编程语言 | Java | 21 (JDK 21) |
| 数据持久化 | Spring Data JPA + Hibernate | 随Spring Boot版本 |
| 数据库 | MySQL | 8.4 |
| 连接池 | HikariCP | 随Spring Boot版本 |
| 容器化 | Docker + Docker Compose | Docker 20.10+ / Compose 2.0+ |
| 构建工具 | Maven | 3.8+ |
| 服务通信 | RestTemplate | 随Spring Boot版本 |

### 3.2 辅助依赖
- **Lombok**: 1.18.32（简化实体类代码）
- **SpringDoc OpenAPI**: 2.3.0（自动生成API文档）
- **Jakarta Validation**: 参数校验
- **curl/jq**: 接口测试与JSON解析（可选）

---

## 4. 环境要求

### 4.1 必需软件
- JDK 21 或更高版本
- Maven 3.8+
- Docker 20.10+
- Docker Compose 2.0+（使用 `docker compose` 命令，非 `docker-compose`）

### 4.2 推荐配置
- 内存：至少 4GB RAM
- 磁盘：至少 2GB 可用空间
- 操作系统：Linux / macOS / Windows (需安装 WSL2)

### 4.3 环境验证
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

## 5. 构建和运行步骤

### 5.1 克隆项目
```bash
git clone <repository-url>
cd course-cloud-three
```

### 5.2 构建项目
在项目根目录执行 Maven 打包命令（跳过测试以加快构建）：
```bash
# 清理并打包所有服务
mvn clean package -DskipTests
```

**打包结果**：
- user-service: `user-service/target/user-service.jar`
- catalog-service: `catalog-service/target/catalog-service.jar`
- enrollment-service: `enrollment-service/target/enrollment-service.jar`

### 5.3 启动服务
使用 Docker Compose 一键启动所有服务（包含3个数据库+3个微服务）：
```bash
# 构建镜像并后台启动所有服务（自定义网络+数据卷持久化）
docker compose up -d --build

# 查看服务运行状态（需6个服务全部正常）
docker compose ps

# 实时查看所有服务日志
docker compose logs -f
```

### 5.4 等待服务启动完成
服务启动需 30-60 秒（数据库初始化 + 应用启动），可通过日志验证：
```bash
# 验证 user-service 启动成功
docker compose logs user-service | grep "Started UserServiceApplication"

# 验证 catalog-service 启动成功
docker compose logs catalog-service | grep "Started CatalogServiceApplication"

# 验证 enrollment-service 启动成功
docker compose logs enrollment-service | grep "Started EnrollmentServiceApplication"
```

### 5.5 服务可用性验证
```bash
# 测试 user-service（应返回学生列表，初始为空数组）
curl http://localhost:8081/api/students

# 测试 catalog-service（应返回课程列表，初始为空数组）
curl http://localhost:8082/api/courses

# 测试 enrollment-service（应返回选课记录列表，初始为空数组）
curl http://localhost:8083/api/enrollments
```

### 5.6 停止服务
```bash
# 停止所有服务（保留数据卷，数据库数据不丢失）
docker compose down

# 停止并删除数据卷（清空数据库，适用于重新测试）
docker compose down -v
```

---

## 6. API 文档

### 6.1 在线文档访问
| 服务名称 | 访问地址 | 说明 |
|----------|----------|------|
| user-service | http://localhost:8081/swagger-ui.html | 学生管理API文档 |
| catalog-service | http://localhost:8082/swagger-ui.html | 课程管理API文档 |
| enrollment-service | http://localhost:8083/swagger-ui.html | 选课管理API文档 |

### 6.2 User Service (端口: 8081)
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

### 6.3 Catalog Service (端口: 8082)
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

### 6.4 Enrollment Service (端口: 8083)
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

## 7. 测试说明

### 7.1 自动化测试脚本
项目提供一键测试脚本 `test-services.sh`，覆盖核心业务场景：
1. **user-service测试**：创建学生、按学号查询学生、验证学生不存在场景
2. **catalog-service测试**：创建课程（含嵌套对象）、按课程代码查询课程、检查课程容量
3. **enrollment-service测试**：
    - 正常选课（验证学生/课程存在性、课程容量）
    - 重复选课（返回错误）
    - 学生/课程不存在选课（返回错误）
    - 退课并验证课程选课人数更新

运行方式：
```bash
# 赋予执行权限
chmod +x test-services.sh

# 运行测试（自动完成全流程验证）
./test-services.sh
```

### 7.2 手动测试流程
#### 步骤1：创建学生（user-service）
```bash
curl -X POST http://localhost:8081/api/students \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "2024001",
    "name": "张三",
    "email": "zhangsan@example.com",
    "major": "计算机科学",
    "grade": 2024
  }'
```

#### 步骤2：创建课程（catalog-service）
```bash
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
```

#### 步骤3：学生选课（enrollment-service，自动调用其他服务验证）
```bash
curl -X POST "http://localhost:8083/api/enrollments?studentId=2024001&courseCode=CS101"
```

#### 步骤4：验证结果
```bash
# 验证课程选课人数已更新
curl http://localhost:8082/api/courses/CS101 | jq '.enrolled'

# 验证学生选课记录存在
curl http://localhost:8083/api/enrollments/student/2024001
```

### 7.3 数据库验证（可选）
```bash
# 连接 user 数据库（学生数据）
docker exec -it user-db mysql -u user_user -puser_pass user_db
SELECT * FROM students;  # 查看学生表

# 连接 catalog 数据库（课程数据）
docker exec -it catalog-db mysql -u catalog_user -pcatalog_pass catalog_db
SELECT * FROM courses;  # 查看课程表

# 连接 enrollment 数据库（选课数据）
docker exec -it enrollment-db mysql -u enrollment_user -penrollment_pass enrollment_db
SELECT * FROM enrollments; # 查看选课记录
```

---

## 8. 容器化部署说明

### 8.1 Dockerfile 配置要点
每个服务的Dockerfile基于`eclipse-temurin:25-jre`构建，示例（以user-service为例）：
```dockerfile
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY target/user-service.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 8.2 docker-compose.yml 核心配置
- **自定义网络**: `course-network`（所有服务加入同一网络，支持服务名访问）
- **数据卷持久化**: `user-data`/`catalog-data`/`enrollment-data`（数据库数据持久化）
- **环境变量配置**: 数据库地址、服务地址通过环境变量注入（避免硬编码）
- **服务依赖**: 微服务依赖数据库健康检查通过后启动
- **数据库配置**: 字符集`utf8mb4`，健康检查`mysqladmin ping`

---

## 9. 项目结构

```
course-cloud-main/
├── user-service/             # 用户服务
│   ├── src/main/java/com/zjgsu/pjt/user/
│   │   ├── controller/       # 接口层（学生相关API）
│   │   ├── service/          # 业务逻辑层
│   │   ├── repository/       # 数据访问层（JPA）
│   │   ├── model/            # 实体类（Student）
│   │   ├── common/           # 异常处理
│   │   └── UserServiceApplication.java  # 应用入口
│   ├── src/main/resources/application.yml  # 配置文件（端口8081，数据库user_db）
│   ├── Dockerfile            # 容器构建文件
│   └── pom.xml               # 服务依赖配置
│
├── catalog-service/          # 课程目录服务
│   ├── src/main/java/com/zjgsu/pjt/catalog/
│   │   ├── controller/       # 接口层（课程相关API）
│   │   ├── service/          # 业务逻辑层
│   │   ├── repository/       # 数据访问层（JPA）
│   │   ├── model/            # 实体类（Course、Instructor、ScheduleSlot）
│   │   ├── common/           # 异常处理
│   │   └── CatalogServiceApplication.java  # 应用入口
│   ├── src/main/resources/application.yml  # 配置文件（端口8082，数据库catalog_db）
│   ├── Dockerfile            # 容器构建文件
│   └── pom.xml               # 服务依赖配置
│
├── enrollment-service/       # 选课服务
│   ├── src/main/java/com/zjgsu/pjt/enrollment/
│   │   ├── controller/       # 接口层（选课相关API）
│   │   ├── service/          # 业务逻辑层（含RestTemplate调用）
│   │   ├── repository/       # 数据访问层（JPA）
│   │   ├── model/            # 实体类（Enrollment）
│   │   ├── config/           # RestTemplate配置
│   │   ├── common/           # 异常处理
│   │   └── EnrollmentServiceApplication.java  # 应用入口
│   ├── src/main/resources/application.yml  # 配置文件（端口8083，服务地址配置）
│   ├── Dockerfile            # 容器构建文件
│   └── pom.xml               # 服务依赖配置
│
├── docker-compose.yml        # 多服务编排配置（3个数据库+3个微服务）
├── test-services.sh          # 自动化测试脚本
└── README.md                 # 项目文档（本文档）
```

---

## 10. 常见问题与解决方案

| 问题现象 | 解决方案 | 可能原因                    |
|----------|----------|-------------------------|
| `command not found: docker-compose` | 安装 Docker Compose V2，使用命令 `docker compose`（无横杠） | 使用了过时的 Docker Compose V1 |
| `healthcheck must be a mapping` | 检查 YAML 文件缩进，确保 `environment`、`healthcheck` 等块层级正确 | docker-compose.yml 缩进错误 |
| 编译失败：`找不到符号: 类 RestController` | 在类上添加 `import org.springframework.web.bind.annotation.RestController;` | 未导入Spring Web注解 |

### 关键解决方案代码示例
#### 1. RestTemplate 配置（enrollment-service）
```java
@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // 添加异常处理器（处理404等状态码）
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                if (response.getRawStatusCode() != 404) {
                    super.handleError(response);
                }
            }
        });
        return restTemplate;
    }
}
```


#### 2. Docker Compose服务地址配置（enrollment-service的application.yml）
```yaml
# 开发环境
user-service:
  url: http://localhost:8081
catalog-service:
  url: http://localhost:8082

# Docker环境（通过环境变量覆盖）
# user-service.url=http://user-service:8081
# catalog-service.url=http://catalog-service:8082
```

---

## 11. 常见操作 FAQ

### Q1: 如何查看服务日志？
```bash
# 查看所有服务日志
docker compose logs

# 查看特定服务日志（实时更新）
docker compose logs -f user-service
docker compose logs -f enrollment-service
```

### Q2: 如何重启单个服务？
```bash
# 重启用户服务
docker compose restart user-service

# 重启选课服务
docker compose restart enrollment-service
```

### Q3: 如何修改服务端口？
编辑 `docker-compose.yml`，修改 `ports` 映射配置：
```yaml
services:
  user-service:
    ports:
      - "9081:8081"  # 本地端口:容器端口（修改本地端口即可）
  catalog-service:
    ports:
      - "9082:8082"
  enrollment-service:
    ports:
      - "9083:8083"
```

### Q4: 如何清空数据库数据？
```bash
# 停止服务并删除数据卷（彻底清空数据库）
docker compose down -v

# 重新启动（将创建全新数据库）
docker compose up -d --build
```

### Q5: 如何本地调试服务？
1. 停止 Docker 中的对应服务：`docker compose stop enrollment-service`
2. 在 IDE 中修改application.yml，使用本地数据库地址
3. 以调试模式启动服务，直接调用本地接口测试