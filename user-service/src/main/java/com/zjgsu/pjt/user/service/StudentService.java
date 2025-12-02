package com.zjgsu.pjt.user.service;

import com.zjgsu.pjt.user.common.BusinessException;
import com.zjgsu.pjt.user.model.Student;
import com.zjgsu.pjt.user.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 学生业务逻辑
 */
@Service
@RequiredArgsConstructor
public class StudentService {
    private final StudentRepository studentRepository;


    /**
     * 按姓名查询学生
     */
    public Student getStudentByUsername(String username) {
        return studentRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException("学生不存在：姓名=" + username, HttpStatus.NOT_FOUND));
    }

//    获取所有学生列表

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    // ... 已有的其他方法 ...
    /**
     * 新增学生（校验学号、邮箱唯一性）
     */
    @Transactional
    public Student createStudent(Student student) {
        // 校验学号唯一性
        if (studentRepository.existsByStudentId(student.getStudentId())) {
            throw new BusinessException("学号已存在：" + student.getStudentId(), HttpStatus.CONFLICT);
        }
        // 校验邮箱唯一性
        if (studentRepository.existsByEmail(student.getEmail())) {
            throw new BusinessException("邮箱已存在：" + student.getEmail(), HttpStatus.CONFLICT);
        }
        return studentRepository.save(student);
    }

    /**
     * 按ID查询学生（不存在则返回404）
     */
    public Student getStudentById(String id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("学生不存在：ID=" + id, HttpStatus.NOT_FOUND));
    }

    /**
     * 按学号查询学生
     */
    public Student getStudentByStudentId(String studentId) {
        return studentRepository.findByStudentId(studentId)
                .orElseThrow(() -> new BusinessException("学生不存在：学号=" + studentId, HttpStatus.NOT_FOUND));
    }

    /**
     * 按专业筛选学生
     */
    public List<Student> getStudentsByMajor(String major) {
        return studentRepository.findByMajor(major);
    }

    /**
     * 更新学生信息（不允许修改学号、邮箱）
     */
    @Transactional
    public Student updateStudent(String id, Student updateStudent) {
        Student existingStudent = getStudentById(id);
        // 禁止修改学号
        if (!existingStudent.getStudentId().equals(updateStudent.getStudentId())) {
            throw new BusinessException("不允许修改学号", HttpStatus.BAD_REQUEST);
        }
        // 禁止修改邮箱
        if (!existingStudent.getEmail().equals(updateStudent.getEmail())) {
            throw new BusinessException("不允许修改邮箱", HttpStatus.BAD_REQUEST);
        }
        // 更新允许修改的字段
        existingStudent.setName(updateStudent.getName());
        existingStudent.setMajor(updateStudent.getMajor());
        existingStudent.setGrade(updateStudent.getGrade());
        return studentRepository.save(existingStudent);
    }

    /**
     * 删除学生（校验是否有关联选课记录）
     */
    @Transactional
    public void deleteStudent(String id) {
        Student student = getStudentById(id);
        // 校验是否有关联选课记录
//        if (enrollmentService.hasEnrollmentsForStudent(id)) {
//            throw new BusinessException("该学生存在选课记录，无法删除", HttpStatus.CONFLICT);
//        }
        studentRepository.delete(student);
    }
}
