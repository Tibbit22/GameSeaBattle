package com.example.demo.repository;

import com.example.demo.model.Field;
import com.example.demo.model.GameSession;
import com.example.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FieldRepository extends JpaRepository<Field, Long> {
    Optional<Field> findByGameAndPlayer(GameSession game, User player);

}
