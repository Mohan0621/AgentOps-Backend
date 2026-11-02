package com.agentops.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class UserAuthException extends RuntimeException {

    public UserAuthException(String msg) {
        super(msg);
    }
}
