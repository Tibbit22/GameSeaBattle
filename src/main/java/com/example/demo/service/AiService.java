package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.FieldRepository;
import com.example.demo.repository.MoveRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiService {
    private final FieldRepository fieldRepository;
    private final MoveRepository moveRepository;
    private final GameLogicService gameLogicService;
    private final Random random = new Random();

    public List<MoveResultWithCoords> makeAiMoves(GameSession game) {
        User aiPlayer = game.getPlayer2();
        List<MoveResultWithCoords> results = new ArrayList<>();
        boolean shouldContinue = true;

        while (shouldContinue && game.getStatus() != GameSession.GameStatus.FINISHED) {
            int x, y;

            do {
                x = random.nextInt(10);
                y = random.nextInt(10);
            } while (isCellAlreadyAttacked(game, x, y));

            log.info("ИИ делает ход ({},{}) в игре {}", x, y, game.getId());

            Move.MoveResult result = Move.MoveResult.MISS;
            try {
                result = gameLogicService.makeMove(game, aiPlayer, x, y);
            } catch (RuntimeException except) {
                log.error(except.getMessage());
            }

            Move move = new Move();
            move.setCreatedAt(LocalDateTime.now());
            move.setGame(game);
            move.setPlayer(aiPlayer);
            move.setX(x);
            move.setY(y);
            move.setResult(result);
            moveRepository.save(move);

            log.info("ИИ делает ход ({},{}) - {} в игре {}", x, y, result, game.getId());

            results.add(new MoveResultWithCoords(result, x, y));

            // Продолжаем ходить только если было попадание или потопление
            shouldContinue = (result == Move.MoveResult.HIT || result == Move.MoveResult.SUNK);
        }

        return results;
    }

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

    private boolean isCellAlreadyAttacked(GameSession game, int x, int y) {
        return moveRepository.existsByGameAndXAndY(game, x, y);
    }

    public void setupAiShips(GameSession game, User aiPlayer) {
        List<List<String>> aiField = generateRandomShipPlacement();
        Field field = new Field();
        field.setGame(game);
        field.setPlayer(aiPlayer);
        field.setShipField(convertFieldToString(aiField));
        fieldRepository.save(field);
        log.info("ИИ сделал расстановку кораблей для игры {}: {}", game.getId(), field.getShipField());
    }

    List<List<String>> generateRandomShipPlacement() {
        List<List<String>> field = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            List<String> row = new ArrayList<>(Collections.nCopies(10, "0"));
            field.add(row);
        }

        placeShip(field, 4);
        placeShip(field, 3);
        placeShip(field, 3);
        placeShip(field, 2);
        placeShip(field, 2);
        placeShip(field, 2);
        placeShip(field, 1);
        placeShip(field, 1);
        placeShip(field, 1);
        placeShip(field, 1);

        return field;
    }

    private void placeShip(List<List<String>> field, int size) {
        boolean placed = false;
        while (!placed) {
            boolean horizontal = random.nextBoolean();
            int x = random.nextInt(10);
            int y = random.nextInt(10);

            if (canPlaceShip(field, x, y, size, horizontal)) {
                for (int i = 0; i < size; i++) {
                    if (horizontal) {
                        field.get(y).set(x + i, "1");
                    } else {
                        field.get(y + i).set(x, "1");
                    }
                }
                placed = true;
            }
        }
    }

    boolean canPlaceShip(List<List<String>> field, int x, int y, int size, boolean horizontal) {
        if (horizontal) {
            if (x + size > 10) return false;
            for (int i = x - 1; i <= x + size; i++) {
                for (int j = y - 1; j <= y + 1; j++) {
                    if (i >= 0 && i < 10 && j >= 0 && j < 10 && field.get(j).get(i).equals("1")) {
                        return false;
                    }
                }
            }
        } else {
            if (y + size > 10) return false;
            for (int i = y - 1; i <= y + size; i++) {
                for (int j = x - 1; j <= x + 1; j++) {
                    if (j >= 0 && j < 10 && i >= 0 && i < 10 && field.get(i).get(j).equals("1")) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private String convertFieldToString(List<List<String>> field) {
        StringBuilder sb = new StringBuilder();
        for (List<String> row : field) {
            for (String cell : row) {
                sb.append(cell);
            }
        }
        return sb.toString();
    }

    @Value
    public static class MoveResultWithCoords {
        Move.MoveResult result;
        int x;
        int y;
    }

    @Value
    private static class Point {
        int x;
        int y;
    }
}