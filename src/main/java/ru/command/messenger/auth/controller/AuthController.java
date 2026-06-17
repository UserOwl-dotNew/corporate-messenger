package ru.command.messenger.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.command.messenger.auth.dto.AuthResponse;
import ru.command.messenger.user.dto.CreateUserRequest;
import ru.command.messenger.auth.dto.LoginRequest;
import ru.command.messenger.user.dto.UserDto;
import ru.command.messenger.auth.service.AuthService;
import ru.command.messenger.user.service.UserService;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/auth")
public class AuthController {
    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto register(@RequestBody @Valid CreateUserRequest request) {
        log.info("Register new user: {}", request.getEmail());
        return userService.createUser(request);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse login(@RequestBody @Valid LoginRequest request) {
        log.info("Login user: {}", request.getUserName());
        return authService;
    }
}
