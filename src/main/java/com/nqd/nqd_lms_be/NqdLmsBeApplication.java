package com.nqd.nqd_lms_be;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
public class NqdLmsBeApplication {

    public static final String TIMEZONE_VN = "Asia/Ho_Chi_Minh";

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(TIMEZONE_VN));
    }

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(TIMEZONE_VN));
        SpringApplication.run(NqdLmsBeApplication.class, args);
    }

}
