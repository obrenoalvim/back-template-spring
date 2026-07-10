package com.example.backtemplate.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app")
@Validated
public class AppProperties {

  @Valid @NotNull @NestedConfigurationProperty private Jwt jwt;

  public Jwt getJwt() {
    return jwt;
  }

  public void setJwt(Jwt jwt) {
    this.jwt = jwt;
  }

  public static class Jwt {

    @NotBlank
    @Size(min = 32, message = "app.jwt.secret must be at least 32 characters")
    private String secret;

    private int accessTtlMinutes = 15;
    private int refreshTtlDays = 30;

    public String getSecret() {
      return secret;
    }

    public void setSecret(String secret) {
      this.secret = secret;
    }

    public int getAccessTtlMinutes() {
      return accessTtlMinutes;
    }

    public void setAccessTtlMinutes(int accessTtlMinutes) {
      this.accessTtlMinutes = accessTtlMinutes;
    }

    public int getRefreshTtlDays() {
      return refreshTtlDays;
    }

    public void setRefreshTtlDays(int refreshTtlDays) {
      this.refreshTtlDays = refreshTtlDays;
    }
  }
}
