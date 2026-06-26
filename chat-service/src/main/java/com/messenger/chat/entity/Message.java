package com.messenger.chat.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Data
@NoArgsConstructor
public class Message {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "chat_id", columnDefinition = "UUID")
    private UUID chatId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_id", insertable = false, updatable = false)
    private Chat chat;

    @Column(name = "sender_id", columnDefinition = "UUID")
    private UUID senderId;

    @Column(name = "encrypted_content", nullable = false)
    private String encryptedContent;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "blockchain_tx_id", length = 255)
    private String blockchainTxId;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "is_deleted")
    private Boolean isDeleted = false;
}