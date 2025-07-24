package com.example.demo.model.dto;

import com.example.demo.model.Move;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MoveResponse {
    private Long id;
    private int x;
    private int y;
    private Move.MoveResult result;
    private LocalDateTime createdAt;
    private PlayerInfo player;

    @Data
    public static class PlayerInfo {
        private Long id;
        private String name;
    }

    public static MoveResponse fromEntity(Move move) {
        MoveResponse response = new MoveResponse();
        response.setId(move.getId());
        response.setX(move.getX());
        response.setY(move.getY());
        response.setResult(move.getResult());
        response.setCreatedAt(move.getCreatedAt());

        PlayerInfo playerInfo = new PlayerInfo();
        playerInfo.setId(move.getPlayer().getId());
        playerInfo.setName(move.getPlayer().getName());
        response.setPlayer(playerInfo);

        return response;
    }
}
