package ru.command.messenger.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class CreateUserRequest {
    @NotBlank(message = "Требуется name")
    private String name;

    @Email(message = "Неправильный формат email")
    @NotBlank(message = "Требуется email")
    private String email;

    @NotBlank(message = "Требуется password")
    private String password;
}
