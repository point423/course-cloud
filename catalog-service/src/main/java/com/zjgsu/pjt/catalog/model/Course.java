package com.zjgsu.pjt.catalog.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 课程实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "课程信息实体类")
@Entity
@Table(name = "courses", indexes = {
        @Index(name = "idx_course_code", columnList = "code")  // 课程代码索引
})
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)  // UUID主键
    @Schema(description = "课程唯一ID（系统自动生成UUID）", example = "123e4567-e89b-12d3-a456-426614174000", required = false, readOnly = true)
    private String id; // 系统生成UUID，作为主键

    @Column(unique = true, nullable = false)
    @Schema(description = "课程编码（如CS101，全局唯一）", example = "CS101", required = true)
    @NotBlank(message = "课程编码不能为空")
    private String code; // 课程编码（如CS101）

    @Column(nullable = false)
    @Schema(description = "课程名称", example = "计算机科学导论", required = true)
    @NotBlank(message = "课程名称不能为空")
    private String title; // 课程名称

    @Embedded
    @Schema(description = "授课教师信息", required = true)
    @Valid // 嵌套校验（校验Instructor的字段）
    private Instructor instructor; // 授课教师

    @Embedded
    @Schema(description = "课程时间安排", required = true)
    @Valid // 嵌套校验（校验ScheduleSlot的字段）
    private Schedule schedule; // 课程时间安排

    @Column(nullable = false)
    @Schema(description = "课程容量（最大选课人数，不能小于1）", example = "60", required = true)
    @Min(value = 1, message = "课程容量不能小于1")
    private Integer capacity; // 课程容量（最大选课人数）

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    @Schema(description = "已选人数（默认0，选课时自动增减）", example = "30", required = false, readOnly = true)
    private Integer enrolled = 0; // 已选人数（默认0，选课时自增）

    @CreationTimestamp  // 自动填充创建时间
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}