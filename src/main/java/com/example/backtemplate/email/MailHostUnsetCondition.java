package com.example.backtemplate.email;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

public class MailHostUnsetCondition implements Condition {

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    String host = context.getEnvironment().getProperty("app.mail.host");
    return !StringUtils.hasText(host);
  }
}
