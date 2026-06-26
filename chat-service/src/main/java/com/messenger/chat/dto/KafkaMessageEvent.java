package com.messenger.chat.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class KafkaMessageEvent {
    private UUID messageId;
    private UUID chatId;
    private UUID senderId;
    private String contentHash;
    private String encryptedContent;
    private Long timestamp;
}