package com.messenger.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ChatRequest {

    private String name;

    @NotNull(message = "Chat type is required")
    private String type; // DIALOG or GROUP

    @NotNull(message = "Participant IDs are required")
    private List<UUID> participantIds;
}