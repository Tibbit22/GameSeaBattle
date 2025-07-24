package com.example.demo.model.dto;

import lombok.Data;

@Data
public class MakeMoveRequest {
    private Long playerId;
    private int x;
    private int y;
}
