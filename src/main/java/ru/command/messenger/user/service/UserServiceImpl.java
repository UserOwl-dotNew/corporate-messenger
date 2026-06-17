package ru.command.messenger.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.command.messenger.exception.DuplicateException;
import ru.command.messenger.exception.NotFoundException;
import ru.command.messenger.user.dto.CreateUserRequest;
import ru.command.messenger.user.dto.UserDto;
import ru.command.messenger.user.mapper.UserMapper;
import ru.command.messenger.user.model.User;
import ru.command.messenger.user.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository repository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserDto createUser(CreateUserRequest requestUser) {
        log.info("Start createUser requestUser={}", requestUser);
        userExistsByEmail(requestUser.getEmail());
        userExistsByName(requestUser.getName());

        User user = mapper.toUser(requestUser);
        user.setPasswordHash(passwordEncoder.encode(requestUser.getPassword()));

        User savedUser = repository.save(user);
        log.info("Пользователь успешно создан с id={}", savedUser.getId());
        return mapper.toUserDto(savedUser);
    }

    @Override
    public UserDto findByUserName(String name) {
        log.info("Start findByUserName name={}", name);
        User findUser = repository.findByName(name)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден с полем name=" + name));
        return mapper.toUserDto(findUser);
    }

    @Override
    public UserDto findByEmail(String email) {
        log.info("Start findByEmail email={}", email);
        User findUser = repository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден с полем email=" + email));
        return mapper.toUserDto(findUser);
    }

    @Override
    public User findByUsernameForAuth(String username) {
        return repository.findByName(username)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден с именем=" + username));
    }

    private void userExistsByEmail(String email) {
        if (repository.existsByEmail(email)) {
            throw new DuplicateException("Пользователь с таким email=" + email + " уже существует");
        }
    }

    private void userExistsByName(String name) {
        if (repository.existsByName(name)) {
            throw new DuplicateException("Пользователь с таким именем=" + name + " уже существует");
        }
    }
}