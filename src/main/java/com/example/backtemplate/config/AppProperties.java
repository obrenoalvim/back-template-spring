package com.example.backtemplate.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
@ConfigurationProperties(prefix = "app")
@Validated
public class AppProperties {

    @NotBlank
    @Size(min = 32, message = "app.jwt.secret must be at least 32 characters")
    private String jwtSecret;

    private int jwtAccessTtlMinutes = 15;
    private int jwtRefreshTtlDays = 30;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public int getJwtAccessTtlMinutes() {
        return jwtAccessTtlMinutes;
    }

    public void setJwtAccessTtlMinutes(int jwtAccessTtlMinutes) {
        this.jwtAccessTtlMinutes = jwtAccessTtlMinutes;
    }

    public int getJwtRefreshTtlDays() {
        return jwtRefreshTtlDays;
    }

    public void setJwtRefreshTtlDays(int jwtRefreshTtlDays) {
        this.jwtRefreshTtlDays = jwtRefreshTtlDays;
    }
}
