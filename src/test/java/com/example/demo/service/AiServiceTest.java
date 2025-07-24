package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.FieldRepository;
import com.example.demo.repository.MoveRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AiServiceTest {

    @Mock
    private FieldRepository fieldRepository;

    @InjectMocks    // создаем экземпляр тестируемого сервиса
    private AiService aiService;

    private GameSession game;
    private User player;
    private User aiPlayer;

    @BeforeEach
    void setUp() {
        player = new User();
        player.setId(1L);
        player.setName("Player");

        aiPlayer = new User();
        aiPlayer.setId(2L);
        aiPlayer.setName("AI Player");

        game = new GameSession();
        game.setId(1L);
        game.setPlayer1(player);
        game.setPlayer2(aiPlayer);
        game.setStatus(GameSession.GameStatus.IN_PROGRESS);
        game.setType(GameSession.GameType.PvE);
    }

    // корректные сохранения сгенерированного поля ИИ в базу данных?
    @Test
    void setupAiShips_ShouldSaveValidField() {
        aiService.setupAiShips(game, aiPlayer);
        // проверяем, что поле сохранилось
        verify(fieldRepository).save(any(Field.class));
    }

    // генерация кораблей корректна?
    @Test
    void generateRandomShipPlacement_ShouldReturnValidField() {
        // генерируем корабли на поле
        List<List<String>> field = aiService.generateRandomShipPlacement();

        assertNotNull(field); // поле не должно быть пустым
        assertEquals(10, field.size()); // должно быть 10 строк
        for (List<String> row : field) {
            assertEquals(10, row.size()); // должно быть 10 клеток
        }
    }

    // нельзя расположить корабли вне поля?
    @Test
    void canPlaceShip_ShouldReturnFalse_WhenShipOutOfBounds() {
        // пустое поле
        List<List<String>> field = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            field.add(new ArrayList<>(Collections.nCopies(10, "0")));
        }
        // проверка размещения корабля за границами поля
        assertFalse(aiService.canPlaceShip(field, 8, 0, 3, true)); // Горизонтальный за границей
        assertFalse(aiService.canPlaceShip(field, 0, 8, 3, false)); // Вертикальный за границей
    }

    // можно ли размещать корабль рядом или поверх другого?
    @Test
    void canPlaceShip_ShouldReturnFalse_WhenShipOverlaps() {
        List<List<String>> field = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            field.add(new ArrayList<>(Collections.nCopies(10, "0")));
        }
        // в пустом поле 1 корабль
        field.get(0).set(0, "1");

        assertFalse(aiService.canPlaceShip(field, 0, 0, 1, true)); // Точно на месте
        assertFalse(aiService.canPlaceShip(field, 1, 1, 1, true)); // Рядом (диагональ)
    }

    // размещение кораблей правильное?
    @Test
    void canPlaceShip_ShouldReturnTrue_WhenValidPlacement() {
        List<List<String>> field = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            field.add(new ArrayList<>(Collections.nCopies(10, "0")));
        }
        // проверка размещения
        assertTrue(aiService.canPlaceShip(field, 0, 0, 1, true)); // Одиночный корабль
        assertTrue(aiService.canPlaceShip(field, 5, 5, 3, false)); // Вертикальный корабль
    }
}
