package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Field {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "game_id")
    private GameSession game;  // Добавьте это поле

    @ManyToOne
    private User player;

    @Column(length = 200) // Достаточно для 10x10 поля
    private String shipField; // Например, "0010001110...", где 100 символов

    public List<List<Integer>> getFieldAsList() {
        List<List<Integer>> result = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            List<Integer> row = new ArrayList<>();
            for (int j = 0; j < 10; j++) {
                row.add(Character.getNumericValue(shipField.charAt(i * 10 + j)));
            }
            result.add(row);
        }
        return result;
    }

    public void setFieldFromList(List<List<Integer>> field) {
        StringBuilder sb = new StringBuilder();
        for (List<Integer> row : field) {
            for (Integer cell : row) {
                sb.append(cell);
            }
        }
        this.shipField = sb.toString();
    }
}
