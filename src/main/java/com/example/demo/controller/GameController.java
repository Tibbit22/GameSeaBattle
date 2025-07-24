package com.example.demo.controller;

import com.example.demo.exception.ActiveGameExistsException;
import com.example.demo.exception.GameFullException;
import com.example.demo.exception.GameNotFoundException;
import com.example.demo.exception.ShipPlacementException;
import com.example.demo.model.GameSession;
import com.example.demo.model.Move;
import com.example.demo.model.Field;
import com.example.demo.model.User;
import com.example.demo.model.dto.*;
import com.example.demo.repository.FieldRepository;
import com.example.demo.repository.GameSessionRepository;
import com.example.demo.repository.MoveRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.GameLogicService;
import com.example.demo.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.model.dto.MoveResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/game")
@Slf4j
public class GameController {
    private final GameService gameService;
    private final MoveRepository moveRepository;
    private final UserRepository userRepository;
    private final GameSessionRepository gameRepository;
    private final FieldRepository fieldRepository;

    // создание игры
    @PostMapping("/create")
    public ResponseEntity<GameSession> createGame(
            @Valid @RequestBody CreateGameRequest request) {
        User creator = gameService.getPlayerById(request.getCreatorId());

        // Проверка на незавершенные игры
        List<GameSession> activeGames = gameService.getUserGameHistory(creator).stream()
                .filter(g -> g.getStatus() != GameSession.GameStatus.FINISHED)
                .toList();

        if (!activeGames.isEmpty()) {
            throw new ActiveGameExistsException("У вас есть незавершенные игры. Завершите их перед созданием новой.");
        }

        GameSession game = gameService.createGame(creator, request.getGameType());
        return ResponseEntity.status(HttpStatus.CREATED).body(game);
    }

    // присоединение к игре
    @PostMapping("/{gameId}/join")
    public ResponseEntity<GameSession> joinGame(
            @PathVariable Long gameId,
            @RequestBody JoinGameRequest request) {
        GameSession game = gameService.getGameById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));
        User player = gameService.getPlayerById(request.getPlayerId());

        if (game.getPlayer2() != null) {
            throw new GameFullException("Игра " + gameId + " уже заполнена");
        }

        if (game.getPlayer1().equals(player)) {
            String errorMessage = "Вы не можете присоединиться к своей собственной игре";
            log.warn(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        // Проверка на незавершенные игры
        List<GameSession> activeGames = gameService.getUserGameHistory(player).stream()
                .filter(g -> g.getStatus() != GameSession.GameStatus.FINISHED)
                .toList();

        if (!activeGames.isEmpty()) {
            String errorMessage = "У вас есть незавершенные игры. Завершите их перед созданием новой.";
            log.warn(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        game.setPlayer2(player);
        game.setStatus(GameSession.GameStatus.IN_PROGRESS);
        return ResponseEntity.ok(gameRepository.save(game));
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameStatus> getGame(@PathVariable Long gameId) {
        GameSession game = gameService.getGameById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));
        return ResponseEntity.ok(new GameStatus(game));
    }

    @GetMapping("/user/{userId}/history")
    public ResponseEntity<List<GameStatus>> getUserGameHistory(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
        List<GameSession> games = gameService.getUserGameHistory(user);
        List<GameStatus> dtos = games.stream()
                .map(GameStatus::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/{gameId}/place-ships/{playerId}")
    public ResponseEntity<?> placeShips(
            @PathVariable Long gameId,
            @PathVariable Long playerId,
            @Valid @RequestBody ShipPlace request) {

        User player = userRepository.findById(playerId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        GameSession game = gameService.getGameById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        if (!game.getPlayer1().equals(player) && !game.getPlayer2().equals(player)) {
            String errorMessage = "Игрок " + playerId + " не участвует в игре " + gameId;
            log.warn(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        Optional<Field> existingField = fieldRepository.findByGameAndPlayer(game, player);
        if (existingField.isPresent()) {
            throw new ShipPlacementException("Расстановка кораблей уже выполнена и не может быть изменена");
        }

        gameService.saveShipPlacement(game, player, request.getField());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{gameId}/moves")
    public ResponseEntity<List<MoveResponse>> getGameMoves(@PathVariable Long gameId) {
        GameSession game = gameService.getGameById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));

        if (game.getStatus() != GameSession.GameStatus.FINISHED) {
            throw new IllegalStateException("Ходы можно просматривать только для завершенных игр");
        }

        List<Move> moves = moveRepository.findByGameOrderByCreatedAtAsc(game);
        List<MoveResponse> response = moves.stream()
                .map(MoveResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}

