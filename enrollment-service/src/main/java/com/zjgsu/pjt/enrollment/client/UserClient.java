package com.zjgsu.pjt.enrollment.client;

import com.zjgsu.pjt.enrollment.dto.StudentDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", fallback = UserClientFallback.class)
public interface UserClient {

    // 这里的路径必须和 user-service 的 Controller 中的路径完全匹配
    @GetMapping("/api/students/studentId/{studentId}")
    StudentDto getStudentByStudentId(@PathVariable("studentId") String studentId);
}