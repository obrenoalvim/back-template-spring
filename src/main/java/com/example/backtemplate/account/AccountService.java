package com.example.backtemplate.account;

import com.example.backtemplate.account.dto.ChangePasswordRequest;
import com.example.backtemplate.auth.PasswordService;
import com.example.backtemplate.auth.UserRepository;
import com.example.backtemplate.common.ApiException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public void changePassword(UUID userId, ChangePasswordRequest req) {
        var user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> ApiException.unauthorized("Invalid session"));
        if (!passwordService.matches(req.currentPassword(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Current password is incorrect");
        }
        user.setPasswordHash(passwordService.hash(req.newPassword()));
        userRepository.save(user);
    }

    public void deleteAccount(UUID userId) {
        userRepository.deleteById(userId);
    }
}
