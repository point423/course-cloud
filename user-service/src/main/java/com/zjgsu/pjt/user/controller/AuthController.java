package com.zjgsu.pjt.user.controller;

import com.zjgsu.pjt.user.model.Student; // 1. 修正：导入正确的 Student 类
import com.zjgsu.pjt.user.service.StudentService;
import com.zjgsu.pjt.user.util.JwtUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final StudentService studentService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // 使用 getStudentByUsername 进行查找，这在新的 Repository 中是支持的
        Student student = studentService.getStudentByUsername(request.getUsername());


        if (student == null || !student.getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户名或密码错误");
        }

        // Token 生成和响应都保持不变，因为我们操作的仍然是 Student 对象
        String token = jwtUtil.generateToken(student.getId(), student.getUsername(), "STUDENT");
        return ResponseEntity.ok(new LoginResponse(token, student));
    }

    /**
     * 登录请求的数据传输对象 (DTO)
     */
    @Data
    static class LoginRequest {
        private String username;
        private String password;
    }

    /**
     * 登录响应的数据传输对象 (DTO)
     */
    @Data
    @AllArgsConstructor
    static class LoginResponse {
        private String token;
        // 5. 修正：字段类型应为 Student
        private Student student;
    }
}
