package com.example.backtemplate.auth;

import com.example.backtemplate.auth.dto.RegisterRequest;
import com.example.backtemplate.common.ApiException;
import com.example.backtemplate.email.EmailService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final EmailService emailService;

    public void register(RegisterRequest req) {
        userRepository
                .findByEmail(req.email())
                .ifPresent(
                        u -> {
                            throw ApiException.conflict("Email already registered");
                        });

        User user = new User();
        user.setEmail(req.email());
        user.setPasswordHash(passwordService.hash(req.password()));
        user.setEmailVerified(false);
        user.setVerificationToken(UUID.randomUUID().toString());
        user.setVerificationTokenExpiresAt(Instant.now().plusSeconds(24 * 3600));
        userRepository.save(user);

        emailService.send(
                user.getEmail(),
                "Verify your email",
                "Verification token: " + user.getVerificationToken());
    }

    public void verifyEmail(String token) {
        User user =
                userRepository
                        .findByVerificationToken(token)
                        .orElseThrow(() -> ApiException.notFound("Invalid verification token"));

        if (user.getVerificationTokenExpiresAt().isBefore(Instant.now())) {
            throw ApiException.conflict("Verification token expired");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiresAt(null);
        userRepository.save(user);
    }
}
