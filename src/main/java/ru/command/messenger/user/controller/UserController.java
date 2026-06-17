package ru.command.messenger.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.command.messenger.user.dto.CreateUserRequest;
import ru.command.messenger.user.dto.UserDto;
import ru.command.messenger.user.service.UserService;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/api/users")
public class UserController {
    private final UserService service;

    @GetMapping("/name/{name}")
    public UserDto getByName(@PathVariable("name") String name) {
        return service.findByUserName(name);
    }

    @GetMapping("/email/{email}")
    public UserDto getByEmail(@PathVariable("email") String email) {
        return service.findByEmail(email);
    }

    @PostMapping
    public UserDto add(@RequestBody @Valid CreateUserRequest req) {
        return service.createUser(req);
    }
}