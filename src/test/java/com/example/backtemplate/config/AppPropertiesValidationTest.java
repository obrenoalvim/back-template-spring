package com.example.backtemplate.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.junit.jupiter.api.Test;

class AppPropertiesValidationTest {

    @Test
    void contextFailsToStartWithoutJwtSecret() {
        var context = new AnnotationConfigApplicationContext();
        TestPropertyValues.of("app.jwt.secret=").applyTo(context);
        context.register(AppProperties.class);

        assertThatThrownBy(context::refresh).hasStackTraceContaining("jwtSecret");

        context.close();
    }
}
