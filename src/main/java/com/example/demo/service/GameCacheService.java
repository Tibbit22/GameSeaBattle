package com.example.demo.service;

import com.example.demo.model.Move;
import com.example.demo.model.GameSession;
import com.example.demo.repository.MoveRepository;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service                    // сервисный компонент
@RequiredArgsConstructor    // генерация конструктора с обязательными аргументами
@Slf4j                      // добавляет логирование
public class GameCacheService {
    private final Cache<Long, List<Move>> movesCache;
    private final MoveRepository moveRepository;

    // Добавление хода в кэш
    public void cacheMove(Long gameId, Move move) {
        if (gameId == null) {
            log.warn("gameId = null!");
            logCacheStats(); // Вывод статистики для отладки
            return;
        }
        log.debug("Попытка кэширования хода для игры {}: ({}, {})", gameId, move.getX(), move.getY());
        // Получаем текущие ходы из кэша или создаем новый список
        List<Move> cachedMoves = Optional.ofNullable(movesCache.getIfPresent(gameId))
                .orElse(new ArrayList<>());
        cachedMoves.add(move);  // добавляем ход в список
        movesCache.put(gameId, cachedMoves); // обновляем кеш

        log.info("Ход успешно сохранён в кэш. Игра: {}, игрок: {}, результат: {}",
                gameId, move.getPlayer().getName(), move.getResult());
    }

    // Получение всех ходов из кэша
    public List<Move> getCachedMoves(Long gameId) {
        // получаем ходы из списка, либо пустой список
        List<Move> moves = Optional.ofNullable(movesCache.getIfPresent(gameId))
                .orElse(new ArrayList<>());

        log.debug("Получено ходов из кэша для игры {}: {}", gameId, moves.size());
        moves.forEach(move -> log.trace("Ход из кэша: {}", move)); // Детали ходов (если нужно)
        return moves;
    }

    // инициализация кеша
    public void initializeCacheForGame(Long gameId) {
        if (gameId == null) {
            log.warn("Попытка инициализации кэша для gameId=null");
            return;
        }
        log.debug("Инициализация кэша для игры {}", gameId);
        // если кеш пуст
        if (movesCache.getIfPresent(gameId) == null) {
            GameSession tempGame = new GameSession();
            tempGame.setId(gameId);
            List<Move> dbMoves = moveRepository.findByGame(tempGame);   // загружаем из БД
            movesCache.put(gameId, new ArrayList<>(dbMoves));           // сохраняем ходы в кеш

            log.info("Кэш инициализирован. Игра: {}, загружено ходов: {}", gameId, dbMoves.size());
            dbMoves.forEach(move -> log.debug("Загружен ход: {}", move)); // Детали ходов
        }
    }
    // логирование статистики кеша
    public void logCacheStats() {
        log.info("Статистика кэша: {}", movesCache.stats());
    }
    // Очистка кэша для конкретной игры
    public void invalidateCache(Long gameId) {
        log.debug("Очистка кэша для игры {}", gameId);
        movesCache.invalidate(gameId);
    }
}