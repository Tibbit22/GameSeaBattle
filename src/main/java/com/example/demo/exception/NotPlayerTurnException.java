package com.example.demo.exception;

public class NotPlayerTurnException extends RuntimeException {
    public NotPlayerTurnException(String playerName) {
        super("Сейчас не ход игрока " + playerName);
    }
}
