package com.example.demo.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShipPlace {
    private static final int SIZE = 10;

    @NotNull
    @Size(min = SIZE, max = SIZE)
    private List<@Size(min = SIZE, max = SIZE) List<@Pattern(regexp = "[01]") String>> field;
}
