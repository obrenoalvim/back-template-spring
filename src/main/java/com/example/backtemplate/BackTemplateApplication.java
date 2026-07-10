package com.example.backtemplate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BackTemplateApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackTemplateApplication.class, args);
    }
}
