package com.zjgsu.pjt.enrollment.client;

import com.zjgsu.pjt.enrollment.common.BusinessException; // 确保你项目中存在这个自定义异常
import com.zjgsu.pjt.enrollment.dto.StudentDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UserClientFallback implements UserClient {

    @Override
    public StudentDto getStudentByStudentId(String studentId) {
        log.warn("UserClient fallback triggered for studentId: {}", studentId);
        // 这里不再返回null，而是直接抛出异常，让全局异常处理器捕获
        throw new BusinessException("用户服务暂时不可用，请稍后再试", HttpStatus.SERVICE_UNAVAILABLE);
    }
}