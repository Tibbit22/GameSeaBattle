package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.FieldRepository;
import com.example.demo.repository.GameSessionRepository;
import com.example.demo.repository.MoveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameLogicServiceTest {
    @Mock
    private MoveRepository moveRepository;
    @Mock
    private FieldRepository fieldRepository;
    @Mock
    private GameSessionRepository gameRepository;
    @Mock
    private GameCacheService gameCacheService;

    @InjectMocks
    private GameLogicService gameLogicService;

    private GameSession game;
    private User player1;
    private User player2;
    private Field player1Field;
    private Field player2Field;

    @BeforeEach
    void setUp() {
        player1 = new User();
        player1.setId(1L);
        player1.setName("Player1");

        player2 = new User();
        player2.setId(2L);
        player2.setName("Player2");

        game = new GameSession();
        game.setId(1L);
        game.setPlayer1(player1);
        game.setPlayer2(player2);
        game.setStatus(GameSession.GameStatus.IN_PROGRESS);
        game.setType(GameSession.GameType.PvP);

        player1Field = new Field();
        player1Field.setGame(game);
        player1Field.setPlayer(player1);
        player1Field.setShipField("00000000000000000000000010000000001000000000100000000010000000000000000000000000000000000000000000"); // 100 chars



        player2Field = new Field();
        player2Field.setGame(game);
        player2Field.setPlayer(player2);
        player2Field.setShipField("1000000000100000000010000000001000000000100000000010000000001000000000000000000000000000000000000000"); // 100 chars
    }

    // устанавливает ли метод первого игрока, когда нет ходов?
    @Test
    void getCurrentPlayer_ShouldReturnPlayer1_WhenNoMoves() {
        when(gameCacheService.getCachedMoves(anyLong())).thenReturn(Collections.emptyList());

        User currentPlayer = gameLogicService.getCurrentPlayer(game);
        assertEquals(player1, currentPlayer);
    }

    // остается ли право хода после попадания?
    @Test
    void getCurrentPlayer_ShouldReturnSamePlayer_AfterHit() {
        Move hitMove = new Move();
        hitMove.setPlayer(player1);
        hitMove.setResult(Move.MoveResult.HIT);

        when(gameCacheService.getCachedMoves(anyLong())).thenReturn(List.of(hitMove));

        // проверка, что ход еще принадлежит первому игроку
        User currentPlayer = gameLogicService.getCurrentPlayer(game);
        assertEquals(player1, currentPlayer);
    }

    // нельзя сделать ход в завершенной игре?
    @Test
    void validateMove_ShouldThrow_WhenGameIsFinished() {
        game.setStatus(GameSession.GameStatus.FINISHED);
        assertThrows(IllegalStateException.class,
                () -> gameLogicService.makeMove(game, player1, 0, 0));
    }

    // сможет ли игрок сделать ход не в свою очередь
    @Test
    void validateMove_ShouldThrow_WhenNotPlayersTurn() {
        // создаем последний ход от player1 с MISS
        Move lastMove = new Move();
        lastMove.setPlayer(player1);
        lastMove.setResult(Move.MoveResult.MISS);
        lastMove.setGame(game);

        // возврат этого хода
        when(gameCacheService.getCachedMoves(game.getId())).thenReturn(List.of(lastMove));

        // проверяем что текущий игрок должен быть player2
        assertEquals(player2, gameLogicService.getCurrentPlayer(game));

        // попытка хода player1 должна вызвать исключение
        assertThrows(IllegalStateException.class,
                () -> gameLogicService.makeMove(game, player1, 0, 0));
    }

    // определяет ли Метод правильно, что все корабли потоплены?
    @Test
    void allShipsSunk_ShouldReturnTrue_WhenAllShipsAreSunk() {
        List<List<Integer>> field = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            List<Integer> row = new ArrayList<>();
            for (int j = 0; j < 10; j++) {
                row.add(0); // Все клетки пустые
            }
            field.add(row);
        }

        assertTrue(gameLogicService.allShipsSunk(field));
    }

    // определяет ли Метод правильно, что не все корабли потоплены?
    @Test
    void allShipsSunk_ShouldReturnFalse_WhenShipExists() {
        List<List<Integer>> field = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            List<Integer> row = new ArrayList<>();
            for (int j = 0; j < 10; j++) {
                row.add(0);
            }
            field.add(row);
        }
        field.get(0).set(0, 1); // Одна клетка с кораблём

        assertFalse(gameLogicService.allShipsSunk(field));
    }

    // после промаха (MISS) ход переходит другому игроку?
    @Test
    void testPlayerSwitchAfterMiss() {
        GameSession game = new GameSession();
        game.setId(1L);

        User player1 = new User();
        player1.setId(79L);
        player1.setName("Player1");

        User player2 = new User();
        player2.setId(80L);
        player2.setName("Player2");

        game.setPlayer1(player1);
        game.setPlayer2(player2);
        game.setType(GameSession.GameType.PvP);

        // создаем тестовый ход с MISS
        Move missMove = new Move();
        missMove.setPlayer(player1);
        missMove.setResult(Move.MoveResult.MISS);
        missMove.setGame(game); // устанавливаем связь с игрой

        when(gameCacheService.getCachedMoves(1L)) // Используем конкретный ID игры
                .thenReturn(List.of(missMove));

        User nextPlayer = gameLogicService.getCurrentPlayer(game);
        // Проверка
        assertEquals(player2.getId(), nextPlayer.getId());
    }

    // после попадания (HIT) ход остается у того же игрока?
    @Test
    void testPlayerStaysAfterHit() {
        GameSession game = new GameSession();
        game.setId(1L);

        User player1 = new User();
        player1.setId(79L);
        User player2 = new User();
        player2.setId(80L);
        game.setPlayer1(player1);
        game.setPlayer2(player2);
        game.setType(GameSession.GameType.PvP);

        Move hitMove = new Move();
        hitMove.setPlayer(player1);
        hitMove.setResult(Move.MoveResult.HIT);

        when(gameCacheService.getCachedMoves(1L)).thenReturn(List.of(hitMove));

        User nextPlayer = gameLogicService.getCurrentPlayer(game);
        assertEquals(player1.getId(), nextPlayer.getId());
    }
}