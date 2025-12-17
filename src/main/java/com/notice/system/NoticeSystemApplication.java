package com.notice.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NoticeSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(NoticeSystemApplication.class, args);
    }

}
