package com.unite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class UniteApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniteApplication.class, args);
    }
}
