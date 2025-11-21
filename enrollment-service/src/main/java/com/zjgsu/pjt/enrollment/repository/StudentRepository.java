package com.zjgsu.pjt.enrollment.repository;

import com.zjgsu.pjt.enrollment.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 学生数据访问（内存存储）
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, String> {
    // 按学号查询（唯一）
    Optional<Student> findByStudentId(String studentId);

    // 按邮箱查询（唯一）
    Optional<Student> findByEmail(String email);

    // 按专业筛选
    List<Student> findByMajor(String major);

    // 按年级筛选
    List<Student> findByGrade(Integer grade);

    // 检查学号是否已存在
    boolean existsByStudentId(String studentId);

    // 检查邮箱是否已存在
    boolean existsByEmail(String email);
}