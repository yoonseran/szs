package com.szs.szsapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class SzsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SzsApiApplication.class, args);
    }

}
