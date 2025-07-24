package com.example.demo.controller;

import com.example.demo.exception.GameNotFoundException;
import com.example.demo.model.GameSession;
import com.example.demo.model.Move;
import com.example.demo.model.User;
import com.example.demo.model.dto.GameStatusResponse;
import com.example.demo.model.dto.MakeMoveRequest;
import com.example.demo.model.dto.MoveResultResponse;
import com.example.demo.service.AiService;
import com.example.demo.service.GameLogicService;
import com.example.demo.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/play")
@Slf4j
public class PlayController {
    private final GameService gameService;

    @PostMapping("/{gameId}/move")
    public ResponseEntity<MoveResultResponse> makeMove(
            @PathVariable Long gameId,
            @Valid @RequestBody MakeMoveRequest request) {
        log.info("Сделан ход игроком {} в игре {} по координатам ({},{})",
                request.getPlayerId(), gameId, request.getX(), request.getY());
        GameSession game = gameService.getGameById(gameId)
                .orElseThrow(() -> new GameNotFoundException(gameId));
        User player = gameService.getPlayerById(request.getPlayerId());

        if ("AI Player".equals(player.getName())) {
            throw new IllegalStateException("Нельзя делать ходы за AI");
        }

        try {
            MoveResultResponse response = gameService.processPlayerMove(gameId, request);

            // Формируем полное сообщение
            StringBuilder message = new StringBuilder();
            if (response.getPlayerResult() != null) {
                message.append("Ваш ход: (")
                        .append(response.getX())  // Используем getX() вместо getPlayerX()
                        .append(",")
                        .append(response.getY())  // Используем getY() вместо getPlayerY()
                        .append(") - ")
                        .append(translateResult(response.getPlayerResult()));
            }

            if (response.getAiMovesMessage() != null) {
                if (message.length() > 0) {
                    message.append(". ");
                }
                message.append("AI атаковал: ").append(response.getAiMovesMessage());
            }

            if (game.getStatus() == GameSession.GameStatus.FINISHED) {
                if (message.length() > 0) {
                    message.append(". ");
                }
                message.append("Игра завершена! Победитель: ").append(game.getWinner().getName());
            }

            response.setMessage(message.toString());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(
                    new MoveResultResponse(null, request.getX(), request.getY(), false, e.getMessage())
            );
        }
    }

    private String translateResult(Move.MoveResult result) {
        switch (result) {
            case HIT: return "Попал";
            case MISS: return "Мимо";
            case SUNK: return "Потопил";
            default: return "Неизвестно";
        }
    }
}


