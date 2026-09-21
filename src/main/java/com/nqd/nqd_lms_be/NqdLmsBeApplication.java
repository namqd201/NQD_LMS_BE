package com.nqd.nqd_lms_be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NqdLmsBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(NqdLmsBeApplication.class, args);
    }

}
