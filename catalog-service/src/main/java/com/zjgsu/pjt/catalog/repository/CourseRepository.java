package com.zjgsu.pjt.catalog.repository;

import com.zjgsu.pjt.catalog.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 课程数据访问（内存存储）
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, String> {
    // 按课程代码查询（唯一）
    Optional<Course> findByCode(String code);

    // 按讲师姓名查询
    List<Course> findByInstructorName(String instructorName);

    // 关键修正：用JPQL明确查询“enrolled < capacity”（比较自身两个字段）
    @Query("SELECT c FROM Course c WHERE c.enrolled < c.capacity")
    List<Course> findByEnrolledLessThanCapacity(); // 方法名可自定义，只要和@Query对应即可

    // 按课程标题模糊查询（关键字匹配）
    List<Course> findByTitleContainingIgnoreCase(String keyword);

    // 检查课程代码是否已存在
    boolean existsByCode(String code);


}