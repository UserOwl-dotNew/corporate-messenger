package ru.command.messenger.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import ru.command.messenger.auth.dto.AuthResponse;
import ru.command.messenger.auth.dto.LoginRequest;
import ru.command.messenger.security.jwt.JwtTokenProvider;
import ru.command.messenger.user.model.User;
import ru.command.messenger.user.service.UserService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserService userService;

    public AuthResponse authenticate(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUserName(),
                        request.getPassword()
                )
        );

        User user = userService.findByUsernameForAuth(request.getUserName());
        String token = tokenProvider.generateToken(authentication);

        log.info("User authenticated successfully: {}", request.getUserName());
        return new AuthResponse(token, user.getName(), user.getEmail());
    }
}