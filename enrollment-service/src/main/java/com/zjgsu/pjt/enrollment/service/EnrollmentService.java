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
import com.zjgsu.pjt.enrollment.client.CatalogClient;
import com.zjgsu.pjt.enrollment.client.UserClient;
import com.zjgsu.pjt.enrollment.dto.CourseDto;
import com.zjgsu.pjt.enrollment.dto.StudentDto;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    // 不再需要 RestTemplate，注入我们新的 Feign Clients
    private final UserClient userClient;
    private final CatalogClient catalogClient;



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
        try {
            // 直接调用，非常清爽！
            StudentDto studentDto = userClient.getStudentByStudentId(studentId);
            System.out.println("请求 User Service 成功, 实例端口: " + studentDto.getPort());
        } catch (Exception e) {
            // 如果 Feign Client 内部或 Fallback 抛出异常，这里会捕获
            // 为了保持和之前一样的业务逻辑，我们可以在这里将它包装成 BusinessException
            throw new BusinessException("验证学生失败: " + e.getMessage(), HttpStatus.NOT_FOUND);
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
        try {
            CourseDto courseDto = catalogClient.getCourseById(courseId);
            System.out.println("请求 Catalog Service 成功, 实例端口: " + courseDto.getPort());
            return courseDto.getCourse();
        } catch (Exception e) {
            throw new BusinessException("获取课程失败: " + e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }


    private void updateCourseEnrolledCount(String courseId, int newCount) {
       // 注意：我们新的Feign Client里没有更新具体数字的方法
        // 之前的逻辑是调用一个接口让课程的 enrolledCount +1
        // 所以这里我们直接调用那个方法即可
        catalogClient.incrementEnrolledCount(courseId);
    }
}