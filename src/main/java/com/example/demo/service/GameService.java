package com.example.demo.service;

import com.example.demo.exception.GameNotFoundException;
import com.example.demo.model.*;
import com.example.demo.model.dto.MakeMoveRequest;
import com.example.demo.model.dto.MoveResultResponse;
import com.example.demo.repository.GameSessionRepository;
import com.example.demo.repository.FieldRepository;
import com.example.demo.repository.MoveRepository;
import com.example.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service            // сервисный компонент
@RequiredArgsConstructor
@Slf4j
public class GameService {
    private final FieldRepository shipRepository;
    private final GameSessionRepository gameRepository;
    private final UserRepository userRepository;
    private final GameLogicService gameLogicService;
    private final AiService aiService;
    private final GameCacheService gameCacheService;
    private final MoveRepository moveRepository;

    // метод обработки хода игрока
    @Transactional
    public MoveResultResponse processPlayerMove(Long gameId, MakeMoveRequest request) {
        GameSession game = gameRepository.findById(gameId)      // получаем игру
                .orElseThrow(() -> new GameNotFoundException(gameId));
        User player = getPlayerById(request.getPlayerId());     // получаем игрока

        // Ход игрока (внутри makeMove ход уже сохраняется в кэш)
        Move.MoveResult playerResult = gameLogicService.makeMove(game, player, request.getX(), request.getY());

        // Логика для PvE
        List<Move.MoveResult> aiResults = new ArrayList<>();
        List<Integer> aiXs = new ArrayList<>();
        List<Integer> aiYs = new ArrayList<>();
        StringBuilder aiMovesMessage = new StringBuilder();

        if (game.getType() == GameSession.GameType.PvE && playerResult == Move.MoveResult.MISS) {
            List<AiService.MoveResultWithCoords> aiMoves = aiService.makeAiMoves(game);

            for (AiService.MoveResultWithCoords aiMove : aiMoves) {
                aiResults.add(aiMove.getResult());
                aiXs.add(aiMove.getX());
                aiYs.add(aiMove.getY());

                if (aiMovesMessage.length() > 0) {
                    aiMovesMessage.append("; ");
                }
                aiMovesMessage.append(String.format("(%d,%d) - %s",
                        aiMove.getX(),
                        aiMove.getY(),
                        translateResult(aiMove.getResult())));
            }
        }

        MoveResultResponse response = new MoveResultResponse(
                playerResult,
                request.getX(),
                request.getY(),
                playerResult == Move.MoveResult.HIT || playerResult == Move.MoveResult.SUNK,
                aiResults.isEmpty() ? null : aiResults.get(0),
                aiXs.isEmpty() ? null : aiXs.get(0),
                aiYs.isEmpty() ? null : aiYs.get(0),
                aiResults,
                aiXs,
                aiYs
        );

        response.setAiMovesMessage(aiMovesMessage.length() > 0 ? aiMovesMessage.toString() : null);

        if (game.getStatus() == GameSession.GameStatus.FINISHED) {
            response.setMessage("Игра завершена! Победитель: " + game.getWinner().getName());
        }

        return response;
    }

    // Вспомогательный метод для перевода результата
    private String translateResult(Move.MoveResult result) {
        switch (result) {
            case HIT: return "Попал";
            case MISS: return "Мимо";
            case SUNK: return "Потопил";
            default: return "Неизвестно";
        }
    }

    // сохранение расстановки кораблей
    public void saveShipPlacement(GameSession game, User player, List<List<String>> field) {
        if (game.getStatus() == GameSession.GameStatus.FINISHED) {
            throw new IllegalStateException("Игра уже завершена");
        }

        Optional<Field> existingField = shipRepository.findByGameAndPlayer(game, player);
        if (existingField.isPresent()) {
            throw new IllegalStateException("Расстановка кораблей уже выполнена и не может быть изменена");
        }

        if (!isValidShipPlacement(field)) {
            throw new IllegalArgumentException("Неверная расстановка кораблей: неверное количество или размер кораблей");
        }
        if (!checkShipSpacing(field)) {
            throw new IllegalArgumentException("Корабли должны находиться на расстоянии минимум 1 клетки друг от друга");
        }

        Field fieldEntity = new Field();
        fieldEntity.setGame(game);
        fieldEntity.setPlayer(player);
        fieldEntity.setShipField(convertToString(field));

        shipRepository.save(fieldEntity);
    }

    // метод проверки расстояния между кораблями
    boolean checkShipSpacing(List<List<String>> field) {
        boolean[][] visited = new boolean[10][10];
        List<Set<Point>> ships = new ArrayList<>();

        // Находим все корабли
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                if (field.get(y).get(x).equals("1") && !visited[y][x]) {
                    Set<Point> ship = new HashSet<>();
                    discoverShip(field, x, y, visited, ship);
                    ships.add(ship);
                }
            }
        }

        // Проверяем расстояние между разными кораблями
        for (int i = 0; i < ships.size(); i++) {
            Set<Point> currentShip = ships.get(i);

            for (Point p : currentShip) {
                // Проверяем все 8 соседних клеток
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) continue;

                        int nx = p.x + dx;
                        int ny = p.y + dy;

                        if (nx >= 0 && nx < 10 && ny >= 0 && ny < 10) {
                            // Если клетка с кораблём и не принадлежит текущему кораблю
                            if (field.get(ny).get(nx).equals("1") && !currentShip.contains(new Point(nx, ny))) {
                                return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    // метод проверки валидности расстановки кораблей
    boolean isValidShipPlacement(List<List<String>> field) {
        // Проверка размера поля
        if (field.size() != 10 || field.stream().anyMatch(row -> row.size() != 10)) {
            return false;
        }

        // Проверка количества и размеров кораблей
        Map<Integer, Integer> shipCounts = new HashMap<>();
        boolean[][] visited = new boolean[10][10];
        int totalShips = 0;

        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                if (field.get(y).get(x).equals("1") && !visited[y][x]) {
                    int size = discoverShips(field, x, y, visited);
                    shipCounts.put(size, shipCounts.getOrDefault(size, 0) + 1);
                    totalShips++;
                }
            }
        }

        // Проверяем общее количество кораблей (должно быть 10)
        if (totalShips != 10) {
            log.error("Неверное общее количество кораблей: {}", totalShips);
            return false;
        }

        // Проверяем количество кораблей каждого типа
        return shipCounts.getOrDefault(4, 0) == 1 &&  // 1 x 4-палубный
                shipCounts.getOrDefault(3, 0) == 2 &&  // 2 x 3-палубных
                shipCounts.getOrDefault(2, 0) == 3 &&  // 3 x 2-палубных
                shipCounts.getOrDefault(1, 0) == 4;    // 4 x 1-палубных
    }

    // создание игры
    @Transactional
    public GameSession createGame(User creator, GameSession.GameType gameType) {
        GameSession game = new GameSession();
        game.setPlayer1(creator);
        game.setType(gameType);
        User aiPlayer = null;

        // Если это PvE, автоматически добавляем ИИ как второго игрока
        if (gameType == GameSession.GameType.PvE) {
            aiPlayer = userRepository.findByName("AI Player")
                    .orElseThrow(() -> new RuntimeException("ИИ игрок не найден"));
            game.setPlayer2(aiPlayer);
            game.setStatus(GameSession.GameStatus.IN_PROGRESS);
        } else {
            game.setStatus(GameSession.GameStatus.WAITING_FOR_PLAYER);
        }

        game.setCreatedAt(LocalDateTime.now());

        // Сначала сохраняем игру, чтобы получить ID
        GameSession savedGame = gameRepository.save(game);

        // Затем инициализируем кэш
        gameCacheService.initializeCacheForGame(savedGame.getId());

        // Для PvE расставляем корабли ИИ после сохранения игры
        if (gameType == GameSession.GameType.PvE) {
            aiService.setupAiShips(savedGame, aiPlayer);
        }

        return savedGame;
    }

    // присоединение к игре
    public GameSession joinGame(Long gameId, User player) {
        GameSession game = gameRepository.findById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        if (game.getPlayer2() != null) {
            throw new IllegalStateException("Игра уже заполнена");
        }

        if (game.getPlayer1().equals(player)) {
            throw new IllegalStateException("Вы не можете присоединится к свойе же игре");
        }

        game.setPlayer2(player);
        // Меняем статус только для PvP игр
        if (game.getType() == GameSession.GameType.PvP) {
            game.setStatus(GameSession.GameStatus.IN_PROGRESS);
        }
        // Инициализация кэша при присоединении
        gameCacheService.initializeCacheForGame(gameId);
        return gameRepository.save(game);
    }

    // конвертация поля в строку из 0 и 1
    private String convertToString(List<List<String>> field) {
        StringBuilder sb = new StringBuilder();
        for (List<String> row : field) {
            for (String cell : row) {
                sb.append(cell);
            }
        }
        return sb.toString();
    }

    // метод нахождения кораблей
    private int discoverShips(List<List<String>> field, int x, int y, boolean[][] visited) {
        Set<Point> ship = new HashSet<>();
        discoverShip(field, x, y, visited, ship);
        return ship.size();
    }

    // Метод для обнаружения корабля целиком
    private void discoverShip(List<List<String>> field, int x, int y,
                              boolean[][] visited, Set<Point> ship) {
        if (x < 0 || x >= 10 || y < 0 || y >= 10 ||
                !field.get(y).get(x).equals("1") || visited[y][x]) {
            return;
        }

        visited[y][x] = true;
        ship.add(new Point(x, y));

        // Проверяем только горизонтальные и вертикальные направления
        discoverShip(field, x+1, y, visited, ship); // право
        discoverShip(field, x-1, y, visited, ship); // лево
        discoverShip(field, x, y+1, visited, ship); // низ
        discoverShip(field, x, y-1, visited, ship); // верх
    }

    public List<GameSession> getUserGameHistory(User user) {
        return gameRepository.findByPlayer1OrPlayer2(user, user);
    }

    public Optional<GameSession> getGameById(Long gameId) {
        return gameRepository.findById(gameId);
    }

    public User getPlayerById(Long playerId) {
        return userRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Игрок не найден"));
    }

    @Value
    static class Point {
        int x;
        int y;
    }
}
