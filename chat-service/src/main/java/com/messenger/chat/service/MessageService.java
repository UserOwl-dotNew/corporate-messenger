package com.messenger.chat.service;

import com.messenger.chat.dto.KafkaMessageEvent;
import com.messenger.chat.dto.MessageRequest;
import com.messenger.chat.dto.MessageResponse;
import com.messenger.chat.dto.WebSocketMessage;
import com.messenger.chat.entity.Message;
import com.messenger.chat.exception.ChatException;
import com.messenger.chat.repository.ChatParticipantRepository;
import com.messenger.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final ChatParticipantRepository participantRepository;
    private final KafkaProducerService kafkaProducerService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public MessageResponse processMessage(MessageRequest request, UUID senderId) {
        log.info("Processing message from user {} in chat {}", senderId, request.getChatId());

        // 1. Проверяем, что пользователь участник чата
        if (!participantRepository.existsByChatIdAndUserId(request.getChatId(), senderId)) {
            throw new ChatException("User is not a participant of this chat");
        }

        // 2. Вычисляем SHA-256 хэш от содержимого
        String contentHash = calculateHash(request.getEncryptedContent());

        // 3. Сохраняем сообщение в БД
        Message message = new Message();
        message.setChatId(request.getChatId());
        message.setSenderId(senderId);
        message.setEncryptedContent(request.getEncryptedContent());
        message.setContentHash(contentHash);
        message.setSentAt(LocalDateTime.now());
        message.setIsDeleted(false);

        Message savedMessage = messageRepository.save(message);
        log.info("Message saved with ID: {}", savedMessage.getId());

        // 4. Отправляем сообщение всем участникам через WebSocket
        WebSocketMessage wsMessage = new WebSocketMessage();
        wsMessage.setChatId(savedMessage.getChatId());
        wsMessage.setSenderId(savedMessage.getSenderId());
        wsMessage.setEncryptedContent(savedMessage.getEncryptedContent());
        wsMessage.setContentHash(savedMessage.getContentHash());
        wsMessage.setTimestamp(savedMessage.getSentAt().toEpochSecond(java.time.ZoneOffset.UTC));

        String destination = "/topic/chat/" + savedMessage.getChatId();
        messagingTemplate.convertAndSend(destination, wsMessage);
        log.info("Message sent to WebSocket topic: {}", destination);

        // 5. Отправляем событие в Kafka для blockchain-adapter
        KafkaMessageEvent kafkaEvent = KafkaMessageEvent.builder()
                .messageId(savedMessage.getId())
                .chatId(savedMessage.getChatId())
                .senderId(savedMessage.getSenderId())
                .contentHash(savedMessage.getContentHash())
                .encryptedContent(savedMessage.getEncryptedContent())
                .timestamp(savedMessage.getSentAt().toEpochSecond(java.time.ZoneOffset.UTC))
                .build();

        kafkaProducerService.sendMessageEvent(kafkaEvent);
        log.info("Message event sent to Kafka");

        return mapToResponse(savedMessage);
    }

    private String calculateHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private MessageResponse mapToResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .chatId(message.getChatId())
                .senderId(message.getSenderId())
                .encryptedContent(message.getEncryptedContent())
                .contentHash(message.getContentHash())
                .blockchainTxId(message.getBlockchainTxId())
                .sentAt(message.getSentAt())
                .isDeleted(message.getIsDeleted())
                .build();
    }
}