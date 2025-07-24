package com.example.demo.model.dto;

import com.example.demo.model.GameSession;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GameStatus {
    private Long id;
    private GameSession.GameType type;
    private GameSession.GameStatus status;
    private Long player1Id;
    private String player1Name;
    private Long player2Id;
    private String player2Name;
    private Long winnerId;
    private String winnerName;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;

    public GameStatus(GameSession game) {
        this.id = game.getId();
        this.type = game.getType();
        this.status = game.getStatus();
        this.player1Id = game.getPlayer1().getId();
        this.player1Name = game.getPlayer1().getName();
        this.player2Id = game.getPlayer2() != null ? game.getPlayer2().getId() : null;
        this.player2Name = game.getPlayer2() != null ? game.getPlayer2().getName() : null;
        this.winnerId = game.getWinner() != null ? game.getWinner().getId() : null;
        this.winnerName = game.getWinner() != null ? game.getWinner().getName() : null;
        this.createdAt = game.getCreatedAt();
        this.finishedAt = game.getFinishedAt();
    }
}
