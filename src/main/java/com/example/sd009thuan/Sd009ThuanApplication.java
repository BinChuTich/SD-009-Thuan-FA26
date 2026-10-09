package com.example.sd009thuan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class Sd009ThuanApplication {

    public static void main(String[] args) {
        SpringApplication.run(Sd009ThuanApplication.class, args);
    }

}
