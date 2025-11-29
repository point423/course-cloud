package com.zjgsu.pjt.enrollment.dto;

import lombok.Data;
import java.util.Map; // <--- 在这里添加这行代码

@Data
public class CourseDto {
    // 课程信息本身，这里也用一个Map来灵活接收
    private Map<String, Object> course;
    private String port; // 用来验证负载均衡
}