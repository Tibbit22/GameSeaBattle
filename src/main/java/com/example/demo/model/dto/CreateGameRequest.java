package com.example.demo.model.dto;

import com.example.demo.model.GameSession;
import lombok.Data;

@Data
public class CreateGameRequest {
    private Long creatorId;
    private GameSession.GameType gameType;
}