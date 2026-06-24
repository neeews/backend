package com.example.neeews;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NeeewsApplication {

    public static void main(String[] args) {
        SpringApplication.run(NeeewsApplication.class, args);
    }

}
