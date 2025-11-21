# 课程选择微服务项目 (Course Cloud)


## 1. 项目简介

### 1.1 项目概述
Course Cloud 是一个课程选择微服务系统，将原单体应用拆分为两个独立的微服务，实现服务解耦、独立部署和数据隔离，提升系统的可扩展性和维护性。

### 1.2 微服务架构说明
| 服务名称 | 端口 | 核心功能 | 数据库 |
|----------|------|----------|--------|
| **catalog-service**（课程目录服务） | 8081 | 课程信息增删改查，提供课程数据接口 | catalog_db (3307) |
| **enrollment-service**（选课服务） | 8082 | 学生管理、选课/退课管理、课程验证 | enrollment_db (3308) |

#### 服务间通信特性
- **通信方式**: RESTful HTTP（基于 RestTemplate）
- **依赖关系**: enrollment-service → catalog-service（单向依赖）
- **数据隔离**: 各服务独立数据库，禁止跨服务直接访问数据库
- **核心交互**: 选课/退课操作需通过 catalog-service 验证课程合法性

---

## 2. 架构图

```
┌─────────────┐
│   客户端    │
└──────┬──────┘
       │
       ├───────────────┐
       │               ▼
       │  ┌─────────────────────┐     ┌─────────────┐
       │  │ catalog-service     │────▶│ catalog_db  │
       │  │ (8081)              │     │ (3307)      │
       │  │ - 课程CRUD          │     └─────────────┘
       │  └─────────────────────┘
       │
       └───────────────┐
                       ▼
              ┌─────────────────────┐     ┌─────────────┐
              │ enrollment-service  │────▶│ enrollment_ │
              │ (8082)              │     │ db (3308)   │
              │ - 学生管理          │     └─────────────┘
              │ - 选课/退课管理     │
              │ - 课程验证(HTTP调用)│
              └─────────────────────┘
```

---

## 3. 技术栈

### 3.1 核心技术栈
| 类别 | 技术选型 | 版本要求 |
|------|----------|----------|
| 后端框架 | Spring Boot | 3.5.6 |
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
cd course-cloud-main
```

### 5.2 构建项目
在项目根目录执行 Maven 打包命令（跳过测试以加快构建）：
```bash
# 清理并打包所有服务
mvn clean package -DskipTests
```

**打包结果**：
- catalog-service: `catalog-service/target/catalog-service.jar`
- enrollment-service: `enrollment-service/target/enrollment-service.jar`

### 5.3 启动服务
使用 Docker Compose 一键启动所有服务（包含数据库）：
```bash
# 构建镜像并后台启动所有服务
docker compose up -d --build

# 查看服务运行状态
docker compose ps

# 实时查看所有服务日志
docker compose logs -f
```

### 5.4 等待服务启动完成
服务启动需 30-60 秒（数据库初始化 + 应用启动），可通过日志验证：
```bash
# 验证 catalog-service 启动成功
docker compose logs catalog-service | grep "Started CatalogServiceApplication"

# 验证 enrollment-service 启动成功
docker compose logs enrollment-service | grep "Started EnrollmentServiceApplication"
```

### 5.5 服务可用性验证
```bash
# 测试 catalog-service（应返回课程列表，初始为空数组）
curl http://localhost:8081/api/courses

# 测试 enrollment-service（应返回学生列表，初始为空数组）
curl http://localhost:8082/api/students
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
| catalog-service | http://localhost:8081/swagger-ui.html | 课程管理API文档 |
| enrollment-service | http://localhost:8082/swagger-ui.html | 学生/选课管理API文档 |

### 6.2 Catalog Service (端口: 8081)
#### 课程管理接口
| 方法 | 路径 | 描述 | 请求体 | 响应 |
|------|------|------|--------|------|
| GET | `/api/courses` | 获取所有课程 | - | 课程列表（JSON数组） |
| GET | `/api/courses/{id}` | 获取单个课程 | - | 课程详情（JSON对象） |
| POST | `/api/courses` | 创建课程 | Course JSON | 新建课程详情 |
| PUT | `/api/courses/{id}` | 更新课程 | Course JSON | 更新后课程详情 |
| DELETE | `/api/courses/{id}` | 删除课程 | - | 204 No Content |

#### Course 数据结构
```json
{
  "id": "uuid",
  "courseName": "数据结构",
  "courseCode": "CS101",
  "credits": 4,
  "maxStudents": 100,
  "enrolled": 0
}
```

#### 接口示例
```bash
# 创建课程
curl -X POST http://localhost:8081/api/courses \
  -H "Content-Type: application/json" \
  -d '{
    "courseName": "数据结构",
    "courseCode": "CS101",
    "credits": 4,
    "maxStudents": 100
  }'

# 获取所有课程
curl http://localhost:8081/api/courses
```

### 6.3 Enrollment Service (端口: 8082)
#### 学生管理接口
| 方法 | 路径 | 描述 | 请求体 | 响应 |
|------|------|------|--------|------|
| GET | `/api/students` | 获取所有学生 | - | 学生列表（JSON数组） |
| GET | `/api/students/{studentId}` | 获取单个学生 | - | 学生详情（JSON对象） |
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

#### 选课管理接口
| 方法 | 路径 | 描述 | 请求参数 | 响应 |
|------|------|------|----------|------|
| POST | `/api/enrollments` | 学生选课 | `courseId`（课程ID）、`studentId`（学生学号） | 选课记录详情 |
| DELETE | `/api/enrollments` | 学生退课 | `courseId`（课程ID）、`studentId`（学生学号） | 204 No Content |
| GET | `/api/enrollments/course/{courseId}` | 查询课程选课记录 | - | 选课记录列表 |
| GET | `/api/enrollments/course/{courseId}/count` | 统计课程选课人数 | - | 选课人数（数字） |
| GET | `/api/enrollments/course/{courseId}/exists` | 检查课程是否有选课记录 | - | 布尔值（true/false） |

#### Enrollment 数据结构
```json
{
  "id": "uuid",
  "courseId": "course-uuid（",
  "studentId": "2024001",
  "status": "ACTIVE",
  "enrolledAt": "2024-11-21T10:30:00"
}
```

#### 接口示例
```bash
# 创建学生
curl -X POST http://localhost:8082/api/students \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "2024001",
    "name": "张三",
    "email": "zhangsan@example.com",
    "major": "计算机科学",
    "grade": 2024
  }'

# 学生选课（先获取课程ID，再执行选课）
COURSE_ID=$(curl -s http://localhost:8081/api/courses | jq -r '.[0].id')
curl -X POST "http://localhost:8082/api/enrollments?courseId=${COURSE_ID}&studentId=2024001"

# 查询课程选课记录
curl http://localhost:8082/api/enrollments/course/${COURSE_ID}
```

---

## 7. 测试说明

### 7.1 自动化测试脚本
项目提供一键测试脚本 `test-services.sh`，包含完整的流程测试：
```bash
# 赋予执行权限
chmod +x test-services.sh

# 运行测试（自动创建课程、学生、选课，验证结果）
./test-services.sh
```

### 7.2 手动测试流程（推荐）
#### 步骤1：创建课程（catalog-service）
```bash
curl -X POST http://localhost:8081/api/courses \
  -H "Content-Type: application/json" \
  -d '{
    "courseName": "数据结构",
    "courseCode": "CS101",
    "credits": 4,
    "maxStudents": 100
  }'
```

#### 步骤2：创建学生（enrollment-service）
```bash
curl -X POST http://localhost:8082/api/students \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "2024001",
    "name": "张三",
    "email": "zhangsan@example.com"
  }'
```

#### 步骤3：学生选课（enrollment-service → 调用 catalog-service 验证）
```bash
# 获取课程ID
COURSE_ID=$(curl -s http://localhost:8081/api/courses | jq -r '.[0].id')

# 执行选课
curl -X POST "http://localhost:8082/api/enrollments?courseId=${COURSE_ID}&studentId=2024001"
```

#### 步骤4：验证结果
```bash
# 验证课程选课人数已更新
curl http://localhost:8081/api/courses/${COURSE_ID} | jq '.enrolled'

# 验证选课记录存在
curl http://localhost:8082/api/enrollments/course/${COURSE_ID}
```

### 7.3 数据库验证（可选）
```bash
# 连接 catalog 数据库（课程数据）
docker exec -it catalog-db mysql -u catalog_user -pcatalog_pass catalog_db
SELECT * FROM courses;  # 查看课程表

# 连接 enrollment 数据库（学生/选课数据）
docker exec -it enrollment-db mysql -u enrollment_user -penrollment_pass enrollment_db
SELECT * FROM students;   # 查看学生表
SELECT * FROM enrollments; # 查看选课记录
```

---
## 8. 常见问题与解决方案

| 问题现象 | 解决方案 | 可能原因                    |
|----------|----------|-------------------------|
| `command not found: docker-compose` | 安装 Docker Compose V2，使用命令 `docker compose`（无横杠） | 使用了过时的 Docker Compose V1 |
| `healthcheck must be a mapping` | 检查 YAML 文件缩进，确保 `environment`、`healthcheck` 等块层级正确 | docker-compose.yml 缩进错误 |
| 编译失败：`找不到符号: 类 RestController` | 在文件里import | 没有import相应依赖  |
| Docker 构建失败：`COPY target/*.jar ... not found` | 先执行 `mvn clean package -DskipTests`，再执行 `docker compose up --build` | 未提前打包生成 JAR 文件      |
| API 返回 404 | 将 `controller`、`service`、`repository` 包的package语句修改，移到主应用类所在包或其子包下 | Spring 组件扫描失败           |
| 连接被重置：`Recv failure: 连接被对方重置` | 等待一会 | 测试时机太早，服务启动需要时间     |
| 事务提交失败：`Could not commit JPA transaction` | 修改至正确路径 | 启动类的package写错了              |
| 服务间通信失败：`Course not found` | Repository 中创建正确的派生查询（如 `findByCourseId`）<br> | 查询方法错误          |
### 关键解决方案代码示例
#### 1. RestTemplate 配置
```java
@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

#### 2. Lombok 注解处理器配置（pom.xml）
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

---

## 9. 项目结构

```
course-cloud-main/
├── catalog-service/          # 课程目录服务
│   ├── src/main/java/com/zjgsu/pjt/catalog/
│   │   ├── controller/       # 接口层（课程相关API）
│   │   ├── service/          # 业务逻辑层
│   │   ├── repository/       # 数据访问层（JPA）
│   │   ├── model/            # 实体类（Course）
│   │   ├── common/        # 异常处理（全局异常、自定义异常）
│   │   └── CatalogServiceApplication.java  # 应用入口
│   ├── src/main/resources/application.yml  # 配置文件
│   ├── Dockerfile            # 容器构建文件
│   └── pom.xml               # 服务依赖配置
│
├── enrollment-service/       # 选课服务
│   ├── src/main/java/com/zjgsu/pjt/enrollment/
│   │   ├── controller/       # 接口层（学生、选课API）
│   │   ├── service/          # 业务逻辑层
│   │   ├── repository/       # 数据访问层（JPA）
│   │   ├── model/            # 实体类（Student、Enrollment）
│   │   ├── common/        # 异常处理
│   │   └── EnrollmentServiceApplication.java  # 应用入口
│   ├── src/main/resources/application.yml  # 配置文件
│   ├── Dockerfile            # 容器构建文件
│   └── pom.xml               # 服务依赖配置
│
├── docker-compose.yml        # 多服务编排配置
├
├── test-services.sh          # 自动化测试脚本
└── README.md                 # 项目文档（本文档）
```

---

## 10. 常见操作 FAQ

### Q1: 如何查看服务日志？
```bash
# 查看所有服务日志
docker compose logs

# 查看特定服务日志（实时更新）
docker compose logs -f catalog-service
docker compose logs -f enrollment-service
```

### Q2: 如何重启单个服务？
```bash
# 重启课程目录服务
docker compose restart catalog-service

# 重启选课服务
docker compose restart enrollment-service
```

### Q3: 如何修改服务端口？
编辑 `docker-compose.yml`，修改 `ports` 映射配置：
```yaml
services:
  catalog-service:
    ports:
      - "9081:8081"  # 本地端口:容器端口（修改本地端口即可）
  enrollment-service:
    ports:
      - "9082:8082"
```

### Q4: 如何清空数据库数据？
```bash
# 停止服务并删除数据卷（彻底清空数据库）
docker compose down -v

# 重新启动（将创建全新数据库）
docker compose up -d --build
```

### Q5: 如何本地调试服务？
1. 停止 Docker 中的对应服务：`docker compose stop catalog-service`
2. 在 IDEA/Eclipse 中以调试模式启动服务（使用本地配置）
3. 确保本地数据库配置与 `application.yml` 一致

---

