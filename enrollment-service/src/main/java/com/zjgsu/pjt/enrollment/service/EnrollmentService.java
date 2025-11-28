package com.zjgsu.pjt.enrollment.service;

import com.zjgsu.pjt.enrollment.common.BusinessException;
import com.zjgsu.pjt.enrollment.common.ResourceNotFoundException;
import com.zjgsu.pjt.enrollment.enums.EnrollmentStatus;
import com.zjgsu.pjt.enrollment.model.Enrollment;
import com.zjgsu.pjt.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpStatus;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final RestTemplate restTemplate; // 通过构造函数注入



    public List<Enrollment> getAllEnrollments() {
        return enrollmentRepository.findAll();
    }

    @Transactional
    public Enrollment enrollCourse(String courseId, String studentId) {
        // 1. 验证学生是否存在
        validateStudentExists(studentId);
        // 2. 调用课程目录服务获取课程信息，并检查课程是否存在
        Map<String, Object> courseData = getCourseData(courseId);
        Integer capacity = (Integer) courseData.get("capacity");
        Integer enrolled = (Integer) courseData.get("enrolled");

        // 3. 检查课程容量
        if (enrolled >= capacity) {
            throw new BusinessException("Course is full",HttpStatus.BAD_REQUEST);
        }

        // 4. 检查重复选课
        if (enrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            throw new BusinessException("Already enrolled in this course",HttpStatus.CONFLICT );
        }

        // 5. 创建选课记录
        Enrollment enrollment = new Enrollment();
        enrollment.setCourseId(courseId);
        enrollment.setStudentId(studentId);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setEnrolledAt(LocalDateTime.now());
        Enrollment saved = enrollmentRepository.save(enrollment);

        // 6. 调用课程目录服务，更新课程的已选人数
        updateCourseEnrolledCount(courseId, enrolled + 1);
        return saved;
    }

    private void validateStudentExists(String studentId) {
        // 直接使用服务名 'user-service'
        String url = "http://user-service/api/students/studentId/" + studentId; // <--- 修改点 1
        try {
            // 返回的已经是 Map 了
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || response.isEmpty()) { // 做个健壮性检查
                throw new BusinessException("学生不存在: " + studentId, HttpStatus.NOT_FOUND);
            }
            // 可以在这里打印一下端口号，来观察负载均衡
            System.out.println("请求 User Service 成功，实例端口: " + response.get("port"));
        } catch (HttpClientErrorException.NotFound e) {
            throw new BusinessException("学生不存在: " + studentId, HttpStatus.NOT_FOUND);
        }
    }


    @Transactional
    public void dropCourse(String courseId, String studentId) {
        Enrollment enrollment = enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new BusinessException("选课记录不存在", HttpStatus.NOT_FOUND));

        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            throw new BusinessException("学生已退该课程",HttpStatus.BAD_REQUEST);
        }

        // 1. 修改本地选课状态
        enrollment.setStatus(EnrollmentStatus.DROPPED);
        enrollmentRepository.save(enrollment);

        // 2. 调用课程目录服务，获取当前已选人数
        Map<String, Object> courseData = getCourseData(courseId);
        Integer enrolled = (Integer) courseData.get("enrolled");

        // 3. 调用课程目录服务，更新课程已选人数（减1）
        updateCourseEnrolledCount(courseId, enrolled - 1);
    }

    public List<Enrollment> getActiveEnrollmentsByCourseId(String courseId) {
        // 验证课程是否存在
        getCourseData(courseId);
        return enrollmentRepository.findByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE);
    }

    public Integer countActiveEnrollments(String courseId) {
        // 验证课程是否存在
        getCourseData(courseId);
        return enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE);
    }

    public boolean hasEnrollmentsForCourse(String courseId) {
        return enrollmentRepository.existsByCourseId(courseId);
    }

    public boolean hasEnrollmentsForStudent(String studentId) {
        return enrollmentRepository.existsByStudentId(studentId);
    }

    // --- 私有辅助方法，用于与 catalog-service 通信 ---

    private Map<String, Object> getCourseData(String courseId) {
       // 直接使用服务名 'catalog-service'
        String url = "http://catalog-service/api/courses/" + courseId; // <--- 修改点 2
        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                throw new ResourceNotFoundException("Course not found with id: " ,courseId);
            }
            // 可以在这里打印一下端口号
            System.out.println("请求 Catalog Service 成功，实例端口: " + response.get("port"));

            // 从返回的 Map 中提取原始的课程数据
            return (Map<String, Object>) response.get("course"); // <--- 修改点 3

        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Course not found with id: " ,courseId);
        }
    }


    private void updateCourseEnrolledCount(String courseId, int newCount) {
        String url = "http://catalog-service/api/courses/" + courseId + "/enroll"; // <--- 修改点 4
        Map<String, Integer> updateData = Map.of("enrolled", newCount);
        try {
            // 使用 PATCH 或 PUT 方法来更新资源
            restTemplate.patchForObject(url, updateData, Void.class);
        } catch (Exception e) {
            // 在生产环境中，这里应该使用更健壮的日志记录，并考虑失败重试或补偿事务
            System.err.println("Failed to update course enrolled count for course " + courseId + ": " + e.getMessage());
        }
    }
}