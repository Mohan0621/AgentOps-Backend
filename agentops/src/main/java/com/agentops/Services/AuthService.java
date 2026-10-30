package com.agentops.Services;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.agentops.Repositories.AuthRepo;
import com.agentops.DTO.LoginRequest;
import com.agentops.DTO.RegisterRequest;
import com.agentops.DTO.AuthResponse;

@Service
public class AuthService {

    private final AuthRepo repo;
    private final PasswordEncoder encoder;

    public AuthService(AuthRepo repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public AuthResponse register(RegisterRequest user) {

        if (repo.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        String hashedPassword = encoder.encode(user.getPass());

        repo.saveUser(user.getEmail(), hashedPassword);

        return new AuthResponse("Registration Successful");
    }

    public AuthResponse login(LoginRequest user) {

        String storedPassword = repo.getUserPassword(user.getEmail());

        if (storedPassword == null) {
            throw new RuntimeException("Invalid Credentials");
        }

        boolean valid = encoder.matches(
            user.getPass(),
            storedPassword
        );

        if (!valid) {
            throw new RuntimeException("Invalid Credentials");
        }

        return new AuthResponse("Login Successful");
    }
}