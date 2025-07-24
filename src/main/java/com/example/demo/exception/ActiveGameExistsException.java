package com.example.demo.exception;

public class ActiveGameExistsException extends RuntimeException {
    public ActiveGameExistsException(String message) {
        super(message);
    }
}
