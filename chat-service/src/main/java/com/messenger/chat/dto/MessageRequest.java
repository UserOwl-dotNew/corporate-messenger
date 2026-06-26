package com.messenger.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class MessageRequest {

    @NotNull(message = "Chat ID is required")
    private UUID chatId;

    @NotBlank(message = "Message content is required")
    private String encryptedContent;
}