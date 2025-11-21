package com.zjgsu.pjt.enrollment.service;

import com.zjgsu.pjt.enrollment.common.BusinessException;
import com.zjgsu.pjt.enrollment.common.ResourceNotFoundException;
import com.zjgsu.pjt.enrollment.enums.EnrollmentStatus;
import com.zjgsu.pjt.enrollment.model.Enrollment;
import com.zjgsu.pjt.enrollment.model.Student;
import com.zjgsu.pjt.enrollment.repository.EnrollmentRepository;
import com.zjgsu.pjt.enrollment.repository.StudentRepository;
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
    private final StudentRepository studentRepository;
    private final RestTemplate restTemplate; // 通过构造函数注入

    // 从 application.properties 或 application.yml 读取 catalog-service 的地址
    @Value("${catalog.service.url}")
    private String catalogServiceUrl;

    public List<Enrollment> getAllEnrollments() {
        return enrollmentRepository.findAll();
    }

    @Transactional
    public Enrollment enrollCourse(String courseId, String studentId) {
        // 1. 验证学生是否存在
        studentRepository.findByStudentId(studentId) // 假设 studentId 是主键
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

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
        String url = catalogServiceUrl + "/api/courses/" + courseId;
        try {
            Map<String, Object> courseResponse = restTemplate.getForObject(url, Map.class);
            // 收到404时，restTemplate会直接抛出异常，所以如果能走到这里，说明courseResponse一定不是null
            return courseResponse; // <-- 直接返回收到的Map
        } catch (HttpClientErrorException.NotFound e) {
            // 如果catalog-service返回404，这里会捕捉到并转换为我们自己的异常
            throw new ResourceNotFoundException("Course not found with id: " ,courseId);
        }
    }


    private void updateCourseEnrolledCount(String courseId, int newCount) {
        String url = catalogServiceUrl + "/api/courses/" + courseId + "/enroll"; // 假设更新人数的API是这个
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