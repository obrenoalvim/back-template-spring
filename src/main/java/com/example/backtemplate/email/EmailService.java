package com.example.backtemplate.email;

public interface EmailService {
  void send(String to, String subject, String body);
}
