package com.zjgsu.pjt.enrollment.repository;

import com.zjgsu.pjt.enrollment.enums.EnrollmentStatus;
import com.zjgsu.pjt.enrollment.model.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 选课记录数据访问（内存存储）
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, String> {
    // 按课程ID+学生ID查询（唯一）
    Optional<Enrollment> findByCourseIdAndStudentId(String courseId, String studentId);

    // 按课程ID+状态查询
    List<Enrollment> findByCourseIdAndStatus(String courseId, EnrollmentStatus status);

    // 按学生ID+状态查询
    List<Enrollment> findByStudentIdAndStatus(String studentId, EnrollmentStatus status);

    // 统计课程活跃人数（状态为ACTIVE）
    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.courseId = :courseId AND e.status = :status")
    Integer countActiveByCourseId(@Param("courseId") String courseId, @Param("status") EnrollmentStatus status);

    // 判断学生是否已选该课程（任意状态）
    boolean existsByCourseIdAndStudentId(String courseId, String studentId);

    // 按课程ID+学生ID删除选课记录
    void deleteByCourseIdAndStudentId(String courseId, String studentId);

    // 新增：检查课程是否有关联的选课记录
    boolean existsByCourseId(String courseId);

    // 新增：检查学生是否有关联的选课记录
    boolean existsByStudentId(String studentId);

    Integer countByCourseIdAndStatus(String courseId, EnrollmentStatus status);

}