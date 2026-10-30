package com.agentops.Exceptions;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GolbalExceptionHandler{
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> handler(MethodArgumentNotValidException err){
        Map<String,Object> errors=new HashMap<>();
        err.getBindingResult().getFieldErrors().forEach(error->errors.put(error.getField(),error.getDefaultMessage()));
        Map<String,Object> response=new HashMap<>();
        response.put("Status",400);
        response.put("message","Validation Error");
        response.put("errors",errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
    @ExceptionHandler(UserAuthException.class)
    public ResponseEntity<Map<String,Object>> handleUserAuth(UserAuthException e){
        Map<String,Object> res=new HashMap<>();
        res.put("status",404);
        res.put("message",e.getMessage());
        return ResponseEntity.status(HttpStatusCode.valueOf(404)).body(res);
    }
}
