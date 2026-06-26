package com.messenger.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ParticipantRequest {

    @NotNull(message = "User IDs are required")
    private List<UUID> userIds;
}