package com.example.demo.repository;

import com.example.demo.model.GameSession;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {
    @Query("SELECT DISTINCT g FROM GameSession g LEFT JOIN FETCH g.moves WHERE g.id = :gameId")
    List<GameSession> findByStatus(GameSession.GameStatus status);
    List<GameSession> findByPlayer1OrPlayer2(User player1, User player2);
}
