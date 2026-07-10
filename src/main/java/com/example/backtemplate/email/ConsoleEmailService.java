package com.example.backtemplate.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Conditional(MailHostUnsetCondition.class)
public class ConsoleEmailService implements EmailService {

    @Override
    public void send(String to, String subject, String body) {
        log.info("[dev email fallback] to={} subject={}\n{}", to, subject, body);
    }
}
