package com.zjgsu.pjt.catalog;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
/**
 * 项目启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
public class CatalogServiceApplication {


    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}