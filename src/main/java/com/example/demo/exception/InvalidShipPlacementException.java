package com.example.demo.exception;

public class InvalidShipPlacementException extends RuntimeException {
    public InvalidShipPlacementException(String message) {
        super(message);
    }
}
