package com.messenger.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class MessageResponse {
    private UUID id;
    private UUID chatId;
    private UUID senderId;
    private String encryptedContent;
    private String contentHash;
    private String blockchainTxId;
    private LocalDateTime sentAt;
    private Boolean isDeleted;
}