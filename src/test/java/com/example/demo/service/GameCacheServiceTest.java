package com.example.demo.service;

import com.example.demo.model.Move;
import com.example.demo.model.GameSession;
import com.example.demo.model.User;
import com.example.demo.repository.MoveRepository;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)     // поддержка моков для тестов
class GameCacheServiceTest {

    @Mock
    private Cache<Long, List<Move>> movesCache;

    @Mock
    private MoveRepository moveRepository;

    @InjectMocks
    private GameCacheService gameCacheService;

    private Long gameId;
    private Move move;
    private User player;

    @BeforeEach     // метод вызывающийся для всех тестов
    void setUp() {
        gameId = 1L;

        player = new User();
        player.setId(1L);
        player.setName("Test Player");

        move = new Move();
        move.setId(1L);
        move.setX(0);
        move.setY(0);
        move.setPlayer(player);
        move.setResult(Move.MoveResult.HIT);

        GameSession game = new GameSession();
        game.setId(gameId);
        move.setGame(game);
    }

    // правильно ли происходит добавление хода (Move) в кеш для указанной игры (gameId)
    @Test
    void cacheMove_ShouldAddMoveToCache() {
        when(movesCache.getIfPresent(gameId)).thenReturn(new ArrayList<>());

        gameCacheService.cacheMove(gameId, move);
        // проверяем, что метод put() был вызван у movesCache с правильными аргументами (ход добавлен в кеш)
        verify(movesCache).put(eq(gameId), anyList());
    }

    // должен проигнорировать попытку кеширования?
    @Test
    void cacheMove_ShouldNotCacheWhenGameIdIsNull() {
        gameCacheService.cacheMove(null, move);
        // ожидаем, что метод не вызвался
        verify(movesCache, never()).put(any(), any());
    }

    // получение ходов из кеша для указанного gameId корректно?
    @Test
    void getCachedMoves_ShouldReturnCachedMoves() {
        // получаем список с тестовым ходом
        List<Move> expectedMoves = List.of(move);
        when(movesCache.getIfPresent(gameId)).thenReturn(expectedMoves);

        List<Move> result = gameCacheService.getCachedMoves(gameId);
        // сравниваем ожидаемый список с рез-том
        assertEquals(expectedMoves, result);
    }

    // происходит ли инициализация кеша данными из БД, если кеш пуст?
    @Test
    void initializeCacheForGame_ShouldLoadMovesFromDbWhenCacheEmpty() {
        List<Move> dbMoves = List.of(move);
        when(movesCache.getIfPresent(gameId)).thenReturn(null); // возвращаем пустой кеш
        when(moveRepository.findByGame(any())).thenReturn(dbMoves); // возвращаем список с ходом

        gameCacheService.initializeCacheForGame(gameId);

        // проверяем, что put() вызван у movesCache с ходами из БД
        verify(movesCache).put(eq(gameId), eq(dbMoves));
    }

    // очистки кеша для указанного gameId корректна?
    @Test
    void invalidateCache_ShouldInvalidateCacheForGame() {
        gameCacheService.invalidateCache(gameId);

        // проверяем, что invalidate() вызван у movesCache с правильным gameId
        verify(movesCache).invalidate(gameId);
    }
}
