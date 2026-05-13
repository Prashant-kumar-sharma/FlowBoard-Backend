package com.flowboard.list;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ListServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ListServiceApplication.class, args);
    }
}
