package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest     // загружает полный контекст Spring для интеграционного тестирования
@Transactional      // каждый тест выполняется в транзакции, которая откатывается после завершения
class GameIntegrationTest {

    @Autowired
    private GameService gameService;

    @Autowired
    private GameLogicService gameLogicService;

    @Autowired
    private AiService aiService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameSessionRepository gameRepository;

    @Autowired
    private FieldRepository fieldRepository;

    @Autowired
    private MoveRepository moveRepository;

    private User humanPlayer;
    private User aiPlayer;
    private GameSession pveGame;

    @BeforeEach
    void setUp() {
        humanPlayer = new User();
        humanPlayer.setName("Test Player");
        humanPlayer = userRepository.save(humanPlayer);

        aiPlayer = new User();
        aiPlayer.setName("AI Player");
        aiPlayer = userRepository.save(aiPlayer);

        pveGame = new GameSession();
        pveGame.setPlayer1(humanPlayer);
        pveGame.setPlayer2(aiPlayer);
        pveGame.setType(GameSession.GameType.PvE);
        pveGame.setStatus(GameSession.GameStatus.IN_PROGRESS);
        pveGame = gameRepository.save(pveGame);
    }

    // полный цикл игры PvE от создания до первого хода с сохранением в БД корректен?
    @Test
    void testPvEGameFlow() {
        // Проверяем создание игры
        assertNotNull(pveGame);
        assertEquals(humanPlayer, pveGame.getPlayer1());
        assertEquals(aiPlayer, pveGame.getPlayer2());
        assertEquals(GameSession.GameType.PvE, pveGame.getType());

        // Проверяем расстановку кораблей AI
        aiService.setupAiShips(pveGame, aiPlayer);
        Field aiField = fieldRepository.findByGameAndPlayer(pveGame, aiPlayer).orElse(null);
        assertNotNull(aiField);
        assertNotNull(aiField.getShipField());
        assertEquals(100, aiField.getShipField().length()); // поле 10 на 10

        // Проверяем текущего игрока (должен быть человек)
        User currentPlayer = gameLogicService.getCurrentPlayer(pveGame);
        assertEquals(humanPlayer, currentPlayer);

        // Делаем ход игрока и сохраняем его
        Move.MoveResult result = gameLogicService.makeMove(pveGame, humanPlayer, 0, 0);
        assertTrue(result == Move.MoveResult.HIT || result == Move.MoveResult.MISS || result == Move.MoveResult.SUNK);

        // Создаем и сохраняем ход вручную, так как makeMove не сохраняет его автоматически
        Move move = new Move();
        move.setGame(pveGame);
        move.setPlayer(humanPlayer);
        move.setX(0);
        move.setY(0);
        move.setResult(result);
        move.setCreatedAt(LocalDateTime.now());
        moveRepository.save(move);

        // Проверяем, что ход сохранился
        List<Move> moves = moveRepository.findByGame(pveGame);
        assertEquals(1, moves.size());
        assertEquals(humanPlayer, moves.get(0).getPlayer());
    }

    // корректное завершение игры после потопления всех кораблей и определение победителя?
    @Test
    void testGameCompletion() {
        // Создаем поле AI с одним однопалубным кораблем (клетка '1')
        Field aiField = new Field();
        aiField.setGame(pveGame);
        aiField.setPlayer(aiPlayer);
        // Все клетки '0', кроме одной '1' в позиции (0,0)
        String aiFieldStr = "1" + String.join("", Collections.nCopies(99, "0"));
        aiField.setShipField(aiFieldStr);
        fieldRepository.save(aiField);

        // Создаем поле игрока
        Field humanField = new Field();
        humanField.setGame(pveGame);
        humanField.setPlayer(humanPlayer);
        humanField.setShipField(String.join("", Collections.nCopies(100, "0")));
        fieldRepository.save(humanField);

        // Делаем ход по единственной клетке с кораблем AI
        Move.MoveResult result = gameLogicService.makeMove(pveGame, humanPlayer, 0, 0);

        // Для однопалубного корабля результат должен быть SUNK
        assertEquals(Move.MoveResult.SUNK, result);

        // Проверяем завершение игры
        GameSession updatedGame = gameRepository.findById(pveGame.getId())
                .orElseThrow();

        assertEquals(GameSession.GameStatus.FINISHED, updatedGame.getStatus(),
                "Игра должна быть завершена после потопления всех кораблей");
        assertEquals(humanPlayer, updatedGame.getWinner(),
                "Игрок должен быть объявлен победителем");
    }

    // корректная автоматическая расстановка кораблей AI по правилам игры?
    @Test
    void testAiShipPlacementValidity() {
        // Генерируем случайную расстановку кораблей AI
        List<List<String>> aiField = aiService.generateRandomShipPlacement();

        // Проверяем валидность расстановки через GameService
        assertTrue(gameService.isValidShipPlacement(aiField));
        assertTrue(gameService.checkShipSpacing(aiField));
    }
}