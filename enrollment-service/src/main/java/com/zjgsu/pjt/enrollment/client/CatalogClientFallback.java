package com.zjgsu.pjt.enrollment.client;

import com.zjgsu.pjt.enrollment.common.BusinessException;
import com.zjgsu.pjt.enrollment.dto.CourseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CatalogClientFallback implements CatalogClient {

    @Override
    public CourseDto getCourseById(String id) {
        log.warn("CatalogClient.getCourseById fallback triggered for courseId: {}", id);
        throw new BusinessException("课程服务暂时不可用，请稍后再试", HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Override
    public void incrementEnrolledCount(String id) {
        log.warn("CatalogClient.incrementEnrolledCount fallback triggered for courseId: {}", id);
        throw new BusinessException("课程服务暂时不可用，无法更新选课人数", HttpStatus.SERVICE_UNAVAILABLE);
    }
}