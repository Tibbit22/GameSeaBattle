package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
public class GameSession {
    public enum GameType { PvP, PvE }
    public enum GameStatus { WAITING_FOR_PLAYER, IN_PROGRESS, FINISHED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnore
    private User player1;

    @ManyToOne
    @JsonIgnore
    private User player2;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner;

    @Enumerated(EnumType.STRING)
    private GameType type;

    @Enumerated(EnumType.STRING)
    private GameStatus status;

    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    private List<Field> fields;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL)
    private List<Move> moves = new ArrayList<>();

    @Override
    public String toString() {
        return "GameSession{" +
                "id=" + id +
                ", type=" + type +
                ", status=" + status +
                ", player1Id=" + (player1 != null ? player1.getId() : null) +
                ", player2Id=" + (player2 != null ? player2.getId() : null) +
                ", winnerId=" + (winner != null ? winner.getId() : null) +
                ", createdAt=" + createdAt +
                ", finishedAt=" + finishedAt +
                '}';
    }
}
