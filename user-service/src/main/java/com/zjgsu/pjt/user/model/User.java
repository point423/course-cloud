package com.zjgsu.pjt.user.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
// 继承设计：单表继承策略
@Entity
@Getter
@Setter
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING)
public abstract class User { // 声明为抽象类，不能被直接实例化
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // 你需要为登录添加 password 字段
    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String username; // 作为登录名

    @Column(unique = true, nullable = false)
    private String email;


}
