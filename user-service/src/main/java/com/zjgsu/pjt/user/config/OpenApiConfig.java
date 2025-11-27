package com.zjgsu.pjt.user.config; // 替换为你的包名（如 zm 是张明的缩写）

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    // 自定义 OpenAPI 文档信息
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // 文档基本信息（标题、描述、版本）
                .info(new Info()
                        .title("校园选课系统 API 文档") // 文档标题
                        .description("基于 Spring Boot 单体架构的校园选课系统，包含课程管理、学生管理、选课管理三大核心模块的 API 接口说明与测试") // 文档描述
                        .version("v1.0.0") // 系统版本
                        .contact(new Contact()
                                .name("pjt")
                                .email("2541703802@example.com")
                                .url("https://github.com/point423")));

    }
}