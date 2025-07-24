package com.example.demo.model.dto;

import com.example.demo.model.GameSession;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GameStatusResponse {
    private GameSession.GameStatus status;
    private Long winnerId;
}

