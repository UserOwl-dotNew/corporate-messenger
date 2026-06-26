package com.messenger.chat.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chat_participants")
@Data
@NoArgsConstructor
@IdClass(ChatParticipantId.class)
public class ChatParticipant {

    @Id
    @Column(name = "chat_id", columnDefinition = "UUID")
    private UUID chatId;

    @Id
    @Column(name = "user_id", columnDefinition = "UUID")
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", insertable = false, updatable = false)
    private Chat chat;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private Role role;

    public enum Role {
        MEMBER, ADMIN
    }
}