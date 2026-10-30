package com.agentops.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.cons0traints.Size;

public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8)
    private String pass;

    public String getEmail() {
        return email;
    }

    public String getPass() {
        return pass;
    }
}