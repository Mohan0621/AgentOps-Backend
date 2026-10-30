package com.agentops.Models;

public class User {

    private Long id;
    private String email;
    private String pass;

    public User() {}

    public User(Long id, String email, String pass) {
        this.id = id;
        this.email = email;
        this.pass = pass;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPass() {
        return pass;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }
}