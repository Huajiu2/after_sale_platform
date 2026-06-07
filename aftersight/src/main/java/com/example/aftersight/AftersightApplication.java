package com.example.aftersight;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.aftersight.mapper")
public class AftersightApplication {

    public static void main(String[] args) {
        SpringApplication.run(AftersightApplication.class, args);
    }

}
