package com.zjgsu.pjt.enrollment.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 嵌入式对象：课程时间安排实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
@Schema(description = "课程时间安排实体类，包含上课时间、星期等信息")
public class Schedule {
    @Column(name = "SCHEDULE_DAY", nullable = false)
    @Schema(description = "星期（枚举值：MONDAY/TUESDAY/WEDNESDAY/THURSDAY/FRIDAY/SATURDAY/SUNDAY）", example = "MONDAY", required = true)
    @NotBlank(message = "星期不能为空")
    private String dayOfWeek;

    @Column(name = "SCHEDULE_START_TIME", nullable = false)
    @Schema(description = "开始时间（格式：HH:MM，如08:00）", example = "08:00", required = true)
    @NotBlank(message = "开始时间不能为空")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "开始时间格式错误（需为HH:MM）")
    private String startTime;

    @Column(name = "SCHEDULE_END_TIME", nullable = false)
    @Schema(description = "结束时间（格式：HH:MM）", example = "10:00", required = true)
    @NotBlank(message = "结束时间不能为空")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "结束时间格式错误（需为HH:MM）")
    private String endTime;


}