package com.zjgsu.pjt.enrollment.dto;

import lombok.Data;

@Data
public class StudentDto {
    // 我们只需要学生信息本身就够了，这里可以简化
    // 假设 user-service 的返回Map中有一个key为 "student" 的学生对象
    private Object student;
    private String port; // 用来验证负载均衡
}