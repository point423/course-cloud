package com.zjgsu.pjt.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Entity
@DiscriminatorValue("STUDENT") // 当 user_type = 'STUDENT' 时，JPA会创建这个对象
@Getter
@Setter
public class Student extends User { // 继承自 User
    @Column(name = "student_id", unique = true)
    private String studentId;

    private String name;
    private String major;
    private Integer grade;
}
