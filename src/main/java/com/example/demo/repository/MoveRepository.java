package com.example.demo.repository;

import com.example.demo.model.GameSession;
import com.example.demo.model.Move;
import com.example.demo.model.User;
import com.example.demo.model.Move.MoveResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MoveRepository extends JpaRepository<Move, Long> {
    List<Move> findByGame(GameSession game);
    boolean existsByGameAndXAndY(GameSession game, int x, int y);
    boolean existsByGameAndPlayerAndXAndY(GameSession game, User player, int x, int y);
    // Добавляем новый метод
    List<Move> findByGameOrderByCreatedAtAsc(GameSession game);
}


