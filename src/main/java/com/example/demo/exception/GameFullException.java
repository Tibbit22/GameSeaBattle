package com.example.demo.exception;

public class GameFullException extends RuntimeException {
    public GameFullException(String message) {
        super(message);
    }
}
