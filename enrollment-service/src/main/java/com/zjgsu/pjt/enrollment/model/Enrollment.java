package com.zjgsu.pjt.enrollment.model;

import com.zjgsu.pjt.enrollment.enums.EnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 选课记录实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "enrollments", uniqueConstraints = {
        // 课程与学生双重唯一约束
        @UniqueConstraint(name = "uk_course_student", columnNames = {"course_id", "student_id"})
}, indexes = {
        @Index(name = "idx_enrollment_course", columnList = "course_id"),
        @Index(name = "idx_enrollment_student", columnList = "student_id"),
        @Index(name = "idx_enrollment_status", columnList = "status")
})
@Schema(description = "选课记录实体类，记录学生与课程的选课关系")
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "选课记录唯一ID（系统自动生成UUID）", example = "123e4567-e89b-12d3-a456-426614174000", required = false, readOnly = true)
    private String id; // 系统生成UUID

    @Column(name = "course_id", nullable = false)
    @Schema(description = "关联的课程ID（对应课程表的ID）", example = "123e4567-e89b-12d3-a456-426614174001", required = true)
    @NotBlank(message = "课程ID不能为空")
    private String courseId; // 关联的课程ID

    @Column(name = "student_id", nullable = false)
    @Schema(description = "关联的学生ID（对应学生表的ID，系统生成的UUID）", example = "123e4567-e89b-12d3-a456-426614174002", required = true)
    @NotBlank(message = "学生ID不能为空")
    private String studentId; // 关联的学生ID（系统生成的UUID，非学号）

    @CreationTimestamp
    @Column(name = "enrolled_at", nullable = false, updatable = false)
    @Schema(description = "选课时间（系统自动生成）", example = "2024-10-01T14:30:00", required = false, readOnly = true)
    private LocalDateTime enrolledAt; // 选课时间（系统生成）

    @Enumerated(EnumType.STRING)  // 枚举类型存储为字符串
    @Column(nullable = false)
    @Schema(description ="选课状态",example ="ACTIVE")
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;  // 选课状态（默认ACTIVE）
}