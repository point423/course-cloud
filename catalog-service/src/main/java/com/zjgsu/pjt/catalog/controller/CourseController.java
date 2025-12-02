package com.zjgsu.pjt.catalog.controller;

import com.zjgsu.pjt.catalog.common.BusinessException;
import com.zjgsu.pjt.catalog.common.ResourceNotFoundException;
import com.zjgsu.pjt.catalog.model.Course;
import com.zjgsu.pjt.catalog.service.CourseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;
import java.util.Optional; // <-- 1. 添加必要的 import

/**
 * 课程管理API（RESTful）
 */
@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
@Tag(name = "课程管理模块", description = "提供课程的创建、查询、更新、删除等操作")
public class CourseController {
    private final CourseService courseService;
    private final Environment environment;
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses()); // 假设你的Service有这个方法
    }
    // 新增课程
    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        return ResponseEntity.status(201).body(courseService.createCourse(course));
    }

    // 按ID查询课程
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCourseById(@PathVariable String id) {
        // 2. 将 service 的返回结果包装成 Optional，然后再调用 orElseThrow
        Course course = Optional.ofNullable(courseService.getCourseById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Course not found",id));

        Map<String, Object> response = new HashMap<>();
        response.put("course", course);
        String port = environment.getProperty("local.server.port");
        response.put("port", "catalog-service running on port: " + port);
        return ResponseEntity.ok(response);
    }

    // 筛选有剩余容量的课程
    @GetMapping("/available")
    public ResponseEntity<List<Course>> getAvailableCourses() {
        return ResponseEntity.ok(courseService.getCoursesWithRemainingCapacity());
    }

    // 更新课程
    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable String id, @RequestBody Course course) {
        return ResponseEntity.ok(courseService.updateCourse(id, course));
    }

    // 删除课程
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable String id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    // 全局异常处理（业务异常返回对应HTTP状态码）
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<String> handleBusinessException(BusinessException e) {
        return ResponseEntity.status(e.getStatus()).body(e.getMessage());
    }
}