package com.zjgsu.pjt.enrollment.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 嵌入式对象:教师实体（课程关联的教师信息）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
@Schema(description = "教师信息实体类，关联课程的授课教师")
public class Instructor {

    @Column(name = "INSTRUCTOR_NAME", nullable = false)
    @Schema(description = "教师姓名", example = "张教授", required = true)
    @NotBlank(message = "教师姓名不能为空")
    private String name; // 教师姓名

    @Column(name = "INSTRUCTOR_EMAIL", nullable = false)
    @Schema(description = "教师邮箱", example = "zhang@example.edu.cn", required = true)
    @NotBlank(message = "教师邮箱不能为空")
    private String email; // 教师邮箱
}