package com.zjgsu.pjt.enrollment.controller;

import com.zjgsu.pjt.enrollment.common.BusinessException;
import com.zjgsu.pjt.enrollment.model.Enrollment;
import com.zjgsu.pjt.enrollment.service.EnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;  // ← 添加这行
import lombok.RequiredArgsConstructor;  // ← 添加这行

import java.util.List;

/**
 * 选课管理API（RESTful）
 */
@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@Tag(name = "选课管理模块", description = "提供学生选课、退课及选课记录查询等操作")
public class EnrollmentController {
    private final EnrollmentService enrollmentService;

    // 【新增的方法】获取所有选课记录
    @GetMapping
    public ResponseEntity<List<Enrollment>> getAllEnrollments() {return ResponseEntity.ok(enrollmentService.getAllEnrollments());
    }
    // 学生选课
    @PostMapping
    public ResponseEntity<Enrollment> enrollCourse(
            @RequestParam String courseId,
            @RequestParam String studentId) {
        // 直接使用传入的 courseId 和 studentId 调用新的 service 方法
        return ResponseEntity.status(201).body(enrollmentService.enrollCourse(courseId, studentId));
    }

    // 学生退课（按课程ID和学生ID）
    @DeleteMapping
    public ResponseEntity<Void> dropCourse(
            @RequestParam String courseId,
            @RequestParam String studentId
    ) {
        enrollmentService.dropCourse(courseId, studentId);
        return ResponseEntity.noContent().build();
    }

    // 按课程ID查询活跃选课记录
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<Enrollment>> getActiveEnrollmentsByCourseId(@PathVariable String courseId) {
        return ResponseEntity.ok(enrollmentService.getActiveEnrollmentsByCourseId(courseId));
    }

    // 统计课程活跃人数
    @GetMapping("/course/{courseId}/count")
    public ResponseEntity<Integer> countActiveEnrollments(@PathVariable String courseId) {
        return ResponseEntity.ok(enrollmentService.countActiveEnrollments(courseId));
    }

    // 全局异常处理
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<String> handleBusinessException(BusinessException e) {
        return ResponseEntity.status(e.getStatus()).body(e.getMessage());
    }
}
