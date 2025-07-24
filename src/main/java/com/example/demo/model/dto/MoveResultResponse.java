package com.example.demo.model.dto;

import com.example.demo.model.Move;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class MoveResultResponse {
    private Move.MoveResult playerResult;
    private int x;
    private int y;
    @JsonProperty("isCurrentPlayer")
    private boolean isCurrentPlayer;
    private String message;
    private Move.MoveResult aiResult;  // Первый ход AI
    private Integer aiX;               // координаты первого хода AI
    private Integer aiY;
    private List<Move.MoveResult> aiResults; // Все ходы AI
    private List<Integer> aiXs;              // Все координаты X AI
    private List<Integer> aiYs;              // Все координаты Y AI
    private String aiMovesMessage;

    public void setMessage(String message) {
        this.message = message;
    }

    // Конструкторы обновить для поддержки message
    public MoveResultResponse(Move.MoveResult playerResult, int x, int y,
                              boolean isCurrentPlayer, String message) {
        this.playerResult = playerResult;
        this.x = x;
        this.y = y;
        this.isCurrentPlayer = isCurrentPlayer;
        this.message = message;
    }

    // Основной конструктор
    public MoveResultResponse(Move.MoveResult playerResult, int x, int y,
                              boolean isCurrentPlayer, Move.MoveResult aiResult,
                              Integer aiX, Integer aiY, List<Move.MoveResult> aiResults,
                              List<Integer> aiXs, List<Integer> aiYs) {
        this.playerResult = playerResult;
        this.x = x;
        this.y = y;
        this.isCurrentPlayer = isCurrentPlayer;
        this.aiResult = aiResult;
        this.aiX = aiX;
        this.aiY = aiY;
        this.aiResults = aiResults;
        this.aiXs = aiXs;
        this.aiYs = aiYs;
    }

    // Упрощенный конструктор для случаев без хода AI
    public MoveResultResponse(Move.MoveResult playerResult, int x, int y, boolean isCurrentPlayer) {
        this.playerResult = playerResult;
        this.x = x;
        this.y = y;
        this.isCurrentPlayer = isCurrentPlayer;
    }

    public String getMessage() {
        if (this.message != null && !this.message.isEmpty()) {
            return this.message;
        }

        StringBuilder sb = new StringBuilder();
        if (playerResult != null) {
            sb.append(switch(playerResult) {
                case HIT -> "Попал!";
                case MISS -> "Мимо.";
                case SUNK -> "Потопил!";
                default -> "";
            });
        }

        if (aiResult != null) {
            sb.append(" AI атаковал (").append(aiX).append(",").append(aiY).append("): ")
                    .append(switch(aiResult) {
                        case HIT -> "Попал!";
                        case SUNK -> "Потопил!";
                        default -> "Мимо.";
                    });
        }
        return sb.toString();
    }
}






