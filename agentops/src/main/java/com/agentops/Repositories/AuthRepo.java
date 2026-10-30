package com.agentops.Repositories;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframwwork.sterotype.Repository;

@Repository
public class AuthRepo{
    private final JdbcTemplate jdbc;

    public AuthRepo(JdbcTemplate jdbc){
        this.jdbc=jdbc;
    }
    public boolean existsByEmail(String Email){
        String Query="Select * from users where email=?";
        Integer count=jdbc.queryForObject(query,Integer.class,email);

        return count!=null&count>0;
    }

    public void saveUser(String email,String pass){
        String query="Insert Into users (email,pass) values (?,?)";
        jdbc.update(query,email,pass);
    }
    public String getUserPassword(String email) {

        String query = "SELECT pass FROM users WHERE email = ?";

        return jdbc.query(query,rs -> rs.next() ? rs.getString("pass") : null, email );
    }

}