package com.messenger.chat.dto;

import com.messenger.chat.entity.Chat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ChatResponse {
    private UUID id;
    private String name;
    private Chat.ChatType type;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private List<ParticipantInfo> participants;
    private MessageInfo lastMessage;

    @Data
    @Builder
    public static class ParticipantInfo {
        private UUID userId;
        private String role;
        private LocalDateTime joinedAt;
    }

    @Data
    @Builder
    public static class MessageInfo {
        private UUID id;
        private UUID senderId;
        private String contentHash;
        private LocalDateTime sentAt;
    }
}