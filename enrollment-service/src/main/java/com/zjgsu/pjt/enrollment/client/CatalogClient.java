package com.zjgsu.pjt.enrollment.client;

import com.zjgsu.pjt.enrollment.dto.CourseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "catalog-service", fallback = CatalogClientFallback.class)
public interface CatalogClient {

    // 路径与 catalog-service 的 Controller 匹配
    @GetMapping("/api/courses/{id}")
    CourseDto getCourseById(@PathVariable("id") String id);

    // 别忘了我们还需要更新课程人数的接口
    @PostMapping("/api/courses/{id}/enroll")
    void incrementEnrolledCount(@PathVariable("id") String id);
}