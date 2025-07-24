package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.GameSessionRepository;
import com.example.demo.repository.MoveRepository;
import com.example.demo.repository.FieldRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameLogicService {
    private static final int BOARD_SIZE = 10;
    private final FieldRepository fieldRepository;
    private final GameSessionRepository gameRepository;
    private final MoveRepository moveRepository;
    private final GameCacheService gameCacheService;


    public Move.MoveResult makeMove(GameSession game, User player, int x, int y) throws RuntimeException {
        validateMove(game, player, x, y);

        User opponent = game.getPlayer1().equals(player) ? game.getPlayer2() : game.getPlayer1();
        Field opponentField = fieldRepository.findByGameAndPlayer(game, opponent)
                .orElseThrow(() -> new IllegalStateException("Поле противника не найдено"));

        List<List<Integer>> field = convertFieldToList(opponentField.getShipField());
        boolean isHit = field.get(y).get(x) == 1;

        Move.MoveResult result = isHit ? Move.MoveResult.HIT : Move.MoveResult.MISS;

        Move move = new Move();
        move.setCreatedAt(LocalDateTime.now());
        move.setGame(game);
        move.setPlayer(player);
        move.setX(x);
        move.setY(y);
        move.setResult(result);
        gameCacheService.cacheMove(game.getId(), move);

        if (isHit) {
            markCellAsHit(opponentField, x, y);
            List<List<Integer>> updatedField = convertFieldToList(opponentField.getShipField());
            boolean isSunk = isShipSunk(updatedField, x, y);

            if (isSunk) {
                result = Move.MoveResult.SUNK;
                if (allShipsSunk(updatedField)) {
                    endGame(game, player);
                    return result;
                }
            }
        }
        return result;
    }

    boolean allShipsSunk(List<List<Integer>> field) {
        for (List<Integer> row : field) {
            for (Integer cell : row) {
                if (cell == 1) {    // 1 - неподбитая клетка с кораблём
                    return false;
                }
            }
        }
        return true;
    }

    // Конвертирует строковое представление поля в двумерный список
    public List<List<Integer>> convertFieldToList(String fieldString) {
        if (fieldString == null || fieldString.length() != 100) {
            throw new IllegalArgumentException("Field string must be exactly 100 characters");
        }

        List<List<Integer>> field = new ArrayList<>();
        for (int y = 0; y < 10; y++) {
            List<Integer> row = new ArrayList<>();
            for (int x = 0; x < 10; x++) {
                char c = fieldString.charAt(y * 10 + x);
                row.add(Character.getNumericValue(c));
            }
            field.add(row);
        }
        return field;
    }

    // Метод помечает клетку как подбитую в базе данных
    private void markCellAsHit(Field field, int x, int y) {
        String fieldString = field.getShipField();
        int index = y * 10 + x;
        if (index >= 0 && index < fieldString.length()) {
            char[] chars = fieldString.toCharArray();
            // Помечаем клетку как подбитую (значение 2)
            chars[index] = '2';
            field.setShipField(new String(chars));
            fieldRepository.save(field);
        }
    }

    // Метод нахождения клеток корабля
    private void findShipCells(List<List<Integer>> field, int x, int y, Set<Point> shipCells) {
        if (x < 0 || x >= 10 || y < 0 || y >= 10 ||
                (field.get(y).get(x) != 1 && field.get(y).get(x) != 2)) {
            return;
        }
        Point p = new Point(x, y);
        if (shipCells.contains(p)) return;

        shipCells.add(p);
        findShipCells(field, x + 1, y, shipCells);
        findShipCells(field, x - 1, y, shipCells);
        findShipCells(field, x, y + 1, shipCells);
        findShipCells(field, x, y - 1, shipCells);
    }

    // Метод проверке потопления корабля
    private boolean isShipSunk(List<List<Integer>> field, int x, int y) {
        Set<Point> shipCells = new HashSet<>();
        findShipCells(field, x, y, shipCells);

        log.debug("Проверка, потоплен ли корабль по координатам ({},{}). Клетки корабля: {}", x, y, shipCells);
        log.debug("Состояние поля вокруг корабля:");
        for (Point p : shipCells) {
            log.debug("Клетка ({},{}): {}", p.x, p.y, field.get(p.y).get(p.x));
        }

        // Проверяем, что все клетки корабля подбиты (значение 2)
        return shipCells.stream().allMatch(p -> field.get(p.y).get(p.x) == 2);
    }

    // Определение текущего игрока
    @Transactional(readOnly = true)
    public User getCurrentPlayer(GameSession game) {
        Optional<Move> lastMove = getLastMoveFromAllSources(game);

        // Добавьте это логирование
        if (lastMove.isPresent()) {
            log.debug("Player1: {}, Player2: {}",
                    game.getPlayer1().getId(),
                    game.getPlayer2().getId());
            log.debug("Last move player: {}", lastMove.get().getPlayer().getId());
        } else {
            log.debug("No moves yet. Player1 starts: {}", game.getPlayer1().getId());
        }

        if (lastMove.isEmpty()) {
            return game.getPlayer1();
        }

        if (lastMove.get().getResult() == Move.MoveResult.HIT ||
                lastMove.get().getResult() == Move.MoveResult.SUNK) {
            return lastMove.get().getPlayer();
        }

        return game.getPlayer1().equals(lastMove.get().getPlayer()) ?
                game.getPlayer2() : game.getPlayer1();
    }

    private Optional<Move> getLastMoveFromAllSources(GameSession game) {
        List<Move> allMoves = gameCacheService.getCachedMoves(game.getId());
        return allMoves.isEmpty()
                ? Optional.empty()
                : Optional.of(allMoves.get(allMoves.size() - 1));
    }

    private void validateMove(GameSession game, User player, int x, int y) {
        // Проверка статуса игры
        if (game.getStatus() == GameSession.GameStatus.FINISHED) {
            throw new IllegalStateException("Игра завершена");
        }

        // Проверка координат
        if (x < 0 || x >= BOARD_SIZE || y < 0 || y >= BOARD_SIZE) {
            throw new IllegalArgumentException("Координаты вне поля");
        }

        // Проверка очереди хода (для PvP)
        if (game.getType() == GameSession.GameType.PvP) {
            User currentPlayer = getCurrentPlayer(game);
            if (!currentPlayer.equals(player)) {
                throw new IllegalStateException("Сейчас не ваш ход");
            }
        }

        // Проверка повторного выстрела
        if (isCellAlreadyAttacked(game, player, x, y)) {
            throw new IllegalStateException("Клетка уже атакована");
        }
    }

    private boolean isCellAlreadyAttacked(GameSession game, User player, int x, int y) {
        // Проверяем только кэш
        return gameCacheService.getCachedMoves(game.getId()).stream()
                .anyMatch(m -> m.getPlayer().equals(player) && m.getX() == x && m.getY() == y);
    }

    @lombok.Value
    private static class Point {
        int x;
        int y;
    }

    // Завершение игры
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void endGame(GameSession game, User winner) {
        // Обновляем статус игры
        game.setStatus(GameSession.GameStatus.FINISHED);
        game.setWinner(winner);
        game.setFinishedAt(LocalDateTime.now());
        gameRepository.saveAndFlush(game);

        // Получаем ходы из кэша
        List<Move> cachedMoves = gameCacheService.getCachedMoves(game.getId());

        // Фильтруем только новые ходы
        List<Move> newMoves = cachedMoves.stream()
                .filter(move -> !moveRepository.existsByGameAndPlayerAndXAndY(
                        move.getGame(),
                        move.getPlayer(),
                        move.getX(),
                        move.getY()))
                .collect(Collectors.toList());

        // Сохраняем только новые ходы
        if (!newMoves.isEmpty()) {
            moveRepository.saveAll(newMoves);
        }

        // Очищаем кэш
        gameCacheService.invalidateCache(game.getId());

        log.info("Игра {} завершена. Сохранено {} новых ходов. Победитель: {}",
                game.getId(), newMoves.size(), winner.getName());
    }
}