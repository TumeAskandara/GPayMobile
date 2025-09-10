package com.example.gpay;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class GPayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GPayApplication.class, args);
    }

}
