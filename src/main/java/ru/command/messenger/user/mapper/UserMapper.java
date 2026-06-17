package ru.command.messenger.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.command.messenger.user.dto.CreateUserRequest;
import ru.command.messenger.user.dto.UserDto;
import ru.command.messenger.user.model.User;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {
    public UserDto toUserDto(User user);

    User toUser(CreateUserRequest createUser);
}
