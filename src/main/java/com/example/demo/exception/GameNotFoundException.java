package com.example.demo.exception;

public class GameNotFoundException extends RuntimeException {
    public GameNotFoundException(Long gameId) {
        super("Игра с ID " + gameId + " не найдена");
    }
}
