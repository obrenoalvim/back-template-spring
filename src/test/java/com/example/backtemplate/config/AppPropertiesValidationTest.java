package com.example.backtemplate.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

class AppPropertiesValidationTest {

  @EnableConfigurationProperties(AppProperties.class)
  @Configuration
  static class TestConfig {}

  @Test
  void contextFailsToStartWithoutJwtSecret() {
    var context = new AnnotationConfigApplicationContext();
    TestPropertyValues.of("app.jwt.secret=").applyTo(context);
    context.register(TestConfig.class);

    assertThatThrownBy(context::refresh).hasStackTraceContaining("must not be blank");

    context.close();
  }
}
