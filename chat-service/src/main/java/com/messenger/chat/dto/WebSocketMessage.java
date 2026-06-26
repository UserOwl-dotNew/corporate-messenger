package com.messenger.chat.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class WebSocketMessage {
    private UUID chatId;
    private UUID senderId;
    private String encryptedContent;
    private String contentHash;
    private Long timestamp;
}