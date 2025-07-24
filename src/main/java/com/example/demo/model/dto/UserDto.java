package com.example.demo.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserDto {
    @NotBlank(message = "Name is mandatory")
    private String name;

    // Добавить другие необходимые поля
    // Например:
    // private String email;
    // private String password;
}
