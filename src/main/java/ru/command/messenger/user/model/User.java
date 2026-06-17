package ru.command.messenger.user.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@ToString
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Email(message = "Неправильный формат email")
    @NotBlank(message = "Поле email не может быть пустым")
    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password")
    private String passwordHash;

    @Column(name = "public_key")
    private String publicKey;

    @Column(name = "created")
    private LocalDateTime createdAt;

    @Column(name = "updated")
    private LocalDateTime updatedAt;
}
