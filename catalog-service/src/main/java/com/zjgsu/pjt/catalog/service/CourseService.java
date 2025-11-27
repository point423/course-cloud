package com.zjgsu.pjt.catalog.service;

import com.zjgsu.pjt.catalog.common.BusinessException;
import com.zjgsu.pjt.catalog.model.Course;
import com.zjgsu.pjt.catalog.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/**
 * 课程业务逻辑
 */
@Slf4j  // 添加这个注解用于日志
@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;


    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    /**
     * 新增课程（校验课程代码唯一性）
     */
    @Transactional
    public Course createCourse(Course course) {
        // 校验课程代码是否已存在
        if (courseRepository.existsByCode(course.getCode())) {
            throw new BusinessException("课程代码已存在：" + course.getCode(), HttpStatus.CONFLICT);
        }
        // 初始化已选人数为0
        if (course.getEnrolled() == null) {
            course.setEnrolled(0);
        }
        return courseRepository.save(course);
    }

    /**
     * 按ID查询课程（不存在则返回404）
     */
    public Course getCourseById(String id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new BusinessException("课程不存在：ID=" + id, HttpStatus.NOT_FOUND));
    }

    /**
     * 筛选有剩余容量的课程
     */
    public List<Course> getCoursesWithRemainingCapacity() {
        return courseRepository.findByEnrolledLessThanCapacity();
    }

    /**
     * 更新课程信息（不允许修改课程代码）
     */
    @Transactional
    public Course updateCourse(String id, Course updateCourse) {
        Course existingCourse = getCourseById(id);
        // 禁止修改课程代码
        if (!existingCourse.getCode().equals(updateCourse.getCode())) {
            throw new BusinessException("不允许修改课程代码", HttpStatus.BAD_REQUEST);
        }
        // 更新允许修改的字段
        existingCourse.setTitle(updateCourse.getTitle());
        existingCourse.setInstructor(updateCourse.getInstructor());
        existingCourse.setSchedule(updateCourse.getSchedule());
        existingCourse.setCapacity(updateCourse.getCapacity());
        // 已选人数不能大于新容量
        if (updateCourse.getEnrolled() != null && updateCourse.getEnrolled() > existingCourse.getCapacity()) {
            throw new BusinessException("已选人数不能大于课程容量", HttpStatus.BAD_REQUEST);
        }
        if (updateCourse.getEnrolled() != null) {
            existingCourse.setEnrolled(updateCourse.getEnrolled());
        }
        return courseRepository.save(existingCourse);
    }

    /**

     * 删除课程（校验是否有关联选课记录，此处简化，实际需关联EnrollmentService）

     */

    @Transactional

    public void deleteCourse(String id) {

        Course course = getCourseById(id);

        // 校验是否有关联选课记录,

//        TODO:if (enrollmentService.hasEnrollmentsForCourse(id)) {
//
//            throw new BusinessException("该课程存在选课记录，无法删除", HttpStatus.CONFLICT);
//
//        }

        courseRepository.delete(course);

    }
    /**
     * 课程选课人数自增（事务保证，避免超容）
     */
    @Transactional
    public void incrementEnrolled(String courseId) {
        Course course = getCourseById(courseId);
        if (course.getEnrolled() >= course.getCapacity()) {
            throw new BusinessException("课程容量已满：" + course.getTitle(), HttpStatus.CONFLICT);
        }
        course.setEnrolled(course.getEnrolled() + 1);
        courseRepository.save(course);
    }

    /**
     * 课程选课人数自减（事务保证）
     */
    @Transactional
    public void decrementEnrolled(String courseId) {
        Course course = getCourseById(courseId);
        if (course.getEnrolled() > 0) {
            course.setEnrolled(course.getEnrolled() - 1);
            courseRepository.save(course);
        }
    }
}