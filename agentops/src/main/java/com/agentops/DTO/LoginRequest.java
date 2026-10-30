package com.agentops.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest{

    @NotBlank@Email
    private String email;

    @NotBlank
    private String pass;

    public String getEmail(){
        return email;
    }
    public String getPass(){
        return pass;
    }
}