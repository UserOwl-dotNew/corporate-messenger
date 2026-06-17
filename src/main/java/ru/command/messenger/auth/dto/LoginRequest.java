package ru.command.messenger.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Требуется имя пользователя")
    private String userName;

    @NotBlank(message = "Требуется пароль")
    private String password;
}
