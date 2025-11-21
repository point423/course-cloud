package com.zjgsu.pjt.enrollment.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 学生实体
 */
@Data//自动生成所有字段的getter和setter方法
@NoArgsConstructor//自动生成无参构造方法
@AllArgsConstructor//自动生成全参构造方法
@Schema(description = "学生实体类")
@Entity
@Table(name = "students", indexes = {
        @Index(name = "idx_student_id", columnList = "student_id"),
        @Index(name = "idx_student_email", columnList = "email")
})
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "学生唯一ID（系统自动生成 UUID）", example = "123e4567-e89b-12d3-a456-426614174000", readOnly = true)
    private String id; // 系统生成UUID

    @Column(name = "student_id", unique = true, nullable = false)
    @Schema(description = "学号（全局唯一，不可重复）", example = "2024001", required = true)
    @NotBlank(message = "学号不能为空")//数据校验,不为 null.校验字符串
    private String studentId; // 学号（全局唯一）

    @Column(nullable = false)
    @Schema(description = "学生姓名", example = "张三", required = true)
    @NotBlank(message = "学生姓名不能为空")
    private String name; // 学生姓名

    @Column(nullable = false)
    @Schema(description = "专业名称", example = "计算机科学与技术", required = true)
    @NotBlank(message = "专业不能为空")
    private String major; // 专业（如计算机科学与技术）

    @Column(nullable = false)
    @Schema(description = "入学年份", example = "2024", required = true)
    @Min(value = 2000, message = "入学年份不能小于2000")//校验Integer、Long.其值大于等于value
    private Integer grade; // 入学年份（如2024）

    @Column(unique = true, nullable = false)
    @Schema(description = "邮箱地址（需符合标准格式）", example = "zhangsan@example.edu.cn", required = true)
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式错误（需包含@和域名）")//校验字符串是否符合邮箱格式
    private String email; // 邮箱

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @Schema(description = "创建时间戳（系统自动生成）", example = "2024-10-01T10:00:00", readOnly = true)
    private LocalDateTime createdAt; // 系统生成创建时间
}