package com.spring.ai.springaitraveller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class SpringAiTravellerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiTravellerApplication.class, args);
    }

}
