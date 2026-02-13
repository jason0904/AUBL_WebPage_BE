package com.aubl.webpage.service;

import com.aubl.webpage.api.dto.UserSignupRequest;
import com.aubl.webpage.domain.entity.UserAccount;
import com.aubl.webpage.domain.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount signup(UserSignupRequest request) {
        validateRequest(request);
        if (userAccountRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "email already exists");
        }

        UserAccount account = new UserAccount();
        account.setEmail(request.email().trim().toLowerCase());
        account.setPassword(passwordEncoder.encode(request.password()));
        account.setName(request.name().trim());
        account.setPhoneNumber(request.phoneNumber());
        account.setRole("USER");
        account.setIsActive(true);
        return userAccountRepository.save(account);
    }

    private void validateRequest(UserSignupRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request is required");
        }
        if (request.email() == null || request.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password is required");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
    }
}
