package com.example.demo.service;

import com.example.demo.exception.GameNotFoundException;
import com.example.demo.model.*;
import com.example.demo.model.dto.MakeMoveRequest;
import com.example.demo.model.dto.MoveResultResponse;
import com.example.demo.repository.*;
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
class GameServiceTest {

    @Mock
    private GameCacheService gameCacheService;

    @Mock
    private FieldRepository fieldRepository;

    @Mock
    private GameSessionRepository gameRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MoveRepository moveRepository;

    @Mock
    private GameLogicService gameLogicService;

    @Mock
    private AiService aiService;

    @InjectMocks
    private GameService gameService;

    private GameSession game;
    private User player1;
    private User player2;
    private MakeMoveRequest moveRequest;

    // начальные данные для всех тестов
    @BeforeEach
    void setUp() {
        // тестовые игроки
        player1 = new User();
        player1.setId(1L);
        player1.setName("Player1");

        player2 = new User();
        player2.setId(2L);
        player2.setName("Player2");
        // новая игра
        game = new GameSession();
        game.setId(1L);
        game.setPlayer1(player1);
        game.setPlayer2(player2);
        game.setStatus(GameSession.GameStatus.IN_PROGRESS);
        game.setType(GameSession.GameType.PvP);
        // запрос хода
        moveRequest = new MakeMoveRequest();
        moveRequest.setPlayerId(1L);
        moveRequest.setX(0);
        moveRequest.setY(0);
    }

    // Проверяем, что метод вернул не-null ответ
    // Проверяем, что в ответе указан ожидаемый результат HIT
    @Test
    void processPlayerMove_ShouldReturnResponse_ForValidMove() {
        when(gameRepository.findById(anyLong()))    // Когда вызывается findById с любым Long
                .thenReturn(Optional.of(game));     // Тогда возвращаем Optional с тестовой игрой
        when(userRepository.findById(anyLong()))
                .thenReturn(Optional.of(player1));
        when(gameLogicService.makeMove(any(), any(), anyInt(), anyInt()))   // Когда вызывается makeMove с любыми параметрами
                .thenReturn(Move.MoveResult.HIT);                            // Тогда возвращаем HIT

        // Вызываем метод
        MoveResultResponse response = gameService.processPlayerMove(1L, moveRequest);

        // Проверяем результаты
        // проверяем что ответ не null
        assertNotNull(response, "Response should not be null");
        // Проверяем, что результат хода - HIT
        assertEquals(Move.MoveResult.HIT, response.getPlayerResult());
    }

    // выбрасывает ли joinGame() исключение IllegalStateException, когда:
    // Игра уже имеет двух игроков (полная)
    // Кто-то пытается присоединиться третьим
    @Test
    void joinGame_ShouldThrow_WhenGameFull() {
        game.setPlayer2(player2);
        // когда вызывается findById, тогда возвращаем optional с игрой
        when(gameRepository.findById(anyLong())).thenReturn(Optional.of(game));
        // проверка исключения, ожидаем что будет вызвано исключение IllegalStateException при вызове joinGame
        assertThrows(IllegalStateException.class,
                () -> gameService.joinGame(1L, new User()));
    }

    // корректно ли метод сохраняет валидную расстановку кораблей?
    @Test
    void saveShipPlacement_ShouldSaveValidPlacement() {
        // создание валидного поля
        List<List<String>> validField = createValidField();
        // имитация отсутствия сохраненного поля
        when(fieldRepository.findByGameAndPlayer(any(), any())).thenReturn(Optional.empty());
        // проверка, что метод не вызывает исключение
        assertDoesNotThrow(() -> gameService.saveShipPlacement(game, player1, validField));
        // проверяем что метод save был вызван
        verify(fieldRepository).save(any(Field.class));
    }

    // возвращает ли метод валидации true для корректной расстановки кораблей?
    // созданное поле createValidField() действительно проходит валидацию?
    @Test
    void isValidShipPlacement_ShouldReturnTrue_ForValidField() {
        List<List<String>> validField = createValidField();
        // вызов метода
        boolean isValid = gameService.isValidShipPlacement(validField);
        // проверяем, положительный ли рез-т валидации
        assertTrue(isValid);
    }

    // корректна ли проверка валидации?
    @Test
    void isValidShipPlacement_ShouldReturnFalse_ForInvalidField() {
        // делаем невалидное поле
        List<List<String>> invalidField = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            invalidField.add(new ArrayList<>(Collections.nCopies(10, "0")));
        }
        boolean isValid = gameService.isValidShipPlacement(invalidField);
        // ожидаем false
        assertFalse(isValid);
    }

    // корректно ли работает проверка расстояния?
    @Test
    void checkShipSpacing_ShouldReturnFalse_WhenShipsTooClose() {
        // создаём пустое поле
        List<List<String>> invalidField = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            invalidField.add(new ArrayList<>(Collections.nCopies(10, "0")));
        }
        // размещаем два корабля вплотную (диагонально)
        invalidField.get(0).set(0, "1"); // Первый корабль
        invalidField.get(1).set(1, "1"); // Второй корабль рядом
        // проверка расстояния
        boolean isValid = gameService.checkShipSpacing(invalidField);
        assertFalse(isValid);
    }

    // вспомогательный метод валидной расстановки кораблей
    private List<List<String>> createValidField() {
        List<List<String>> field = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            field.add(new ArrayList<>(Collections.nCopies(10, "0")));
        }

        // 4-палубный
        for (int i = 0; i < 4; i++) field.get(0).set(i, "1");
        // 3-палубные
        for (int i = 0; i < 3; i++) field.get(2).set(i, "1");
        for (int i = 0; i < 3; i++) field.get(4).set(i, "1");
        // 2-палубные
        for (int i = 0; i < 2; i++) field.get(6).set(i, "1");
        for (int i = 0; i < 2; i++) field.get(8).set(i, "1");
        for (int i = 0; i < 2; i++) field.get(0).set(i+6, "1");
        // 1-палубные
        field.get(2).set(5, "1");
        field.get(4).set(5, "1");
        field.get(6).set(5, "1");
        field.get(8).set(5, "1");

        return field;
    }
}
