
// ResourceNotFoundException.java（资源不存在异常）
package com.zjgsu.pjt.user.common;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, String id) {
        super(resource + " not found with id: " + id);
    }
}