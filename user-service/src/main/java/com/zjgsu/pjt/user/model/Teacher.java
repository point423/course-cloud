package com.zjgsu.pjt.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("TEACHER") // 当 user_type = 'TEACHER' 时
@Getter
@Setter
public class Teacher extends User { // 继承自 User
    @Column(name = "teacher_id", unique = true)
    private String teacherId;

    private String name;
    private String department;
}
