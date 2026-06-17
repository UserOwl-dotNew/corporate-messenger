package ru.command.messenger.user.service;

import org.springframework.stereotype.Service;
import ru.command.messenger.user.dto.CreateUserRequest;
import ru.command.messenger.user.dto.UserDto;
import ru.command.messenger.user.model.User;

@Service
public interface UserService {
    UserDto createUser(CreateUserRequest user);

    UserDto findByUserName(String name);

    UserDto findByEmail(String email);

    User findByUsernameForAuth(String username);
}
