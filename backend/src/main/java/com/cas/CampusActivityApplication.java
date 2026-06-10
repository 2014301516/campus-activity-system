package com.cas;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cas.mapper")
public class CampusActivityApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusActivityApplication.class, args);
        System.out.println("========================================");
        System.out.println("  校园活动管理系统后端启动成功！");
        System.out.println("  http://localhost:8080");
        System.out.println("========================================");
    }
}
