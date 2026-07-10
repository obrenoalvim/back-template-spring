package com.example.backtemplate.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordServiceTest {

    private final PasswordService passwordService = new PasswordService();

    @Test
    void hashesAndVerifiesPassword() {
        String hash = passwordService.hash("s3cret-passw0rd");

        assertThat(hash).isNotEqualTo("s3cret-passw0rd");
        assertThat(passwordService.matches("s3cret-passw0rd", hash)).isTrue();
        assertThat(passwordService.matches("wrong", hash)).isFalse();
    }
}
