package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Data
@Table(uniqueConstraints = {
        @UniqueConstraint(
                name = "UK_move_unique",
                columnNames = {"game_id", "player_id", "x", "y"}
        )})
public class Move {
    public enum MoveResult {
        HIT, MISS, INVALID, SUNK
    }

    @Transient
    public String getConstraintViolationMessage() {
        return "Вы уже стреляли в клетку (" + x + "," + y + ")";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    @JsonIgnore // Добавляем эту аннотацию
    private GameSession game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    @JsonIdentityReference(alwaysAsId = true)
    private User player;

    private int x;
    private int y;

    @Enumerated(EnumType.STRING)
    private MoveResult result;

    public MoveResult getResult() {
        return result;
    }

    private LocalDateTime createdAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Move move = (Move) o;
        return x == move.x &&
                y == move.y &&
                Objects.equals(game, move.game) &&
                Objects.equals(player, move.player);
    }

    @Override
    public int hashCode() {
        return Objects.hash(game, player, x, y);
    }
}
