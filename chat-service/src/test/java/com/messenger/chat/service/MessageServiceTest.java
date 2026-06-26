package com.messenger.chat.service;

import com.messenger.chat.dto.KafkaMessageEvent;
import com.messenger.chat.dto.MessageRequest;
import com.messenger.chat.dto.MessageResponse;
import com.messenger.chat.dto.WebSocketMessage;
import com.messenger.chat.entity.Message;
import com.messenger.chat.exception.ChatException;
import com.messenger.chat.repository.ChatParticipantRepository;
import com.messenger.chat.repository.MessageRepository;
import com.messenger.chat.service.KafkaProducerService;
import com.messenger.chat.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ChatParticipantRepository participantRepository;

    @Mock
    private KafkaProducerService kafkaProducerService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private MessageService messageService;

    private UUID chatId;
    private UUID senderId;
    private String encryptedContent;

    @BeforeEach
    void setUp() {
        chatId = UUID.randomUUID();
        senderId = UUID.randomUUID();
        encryptedContent = "SGVsbG8gV29ybGQ=";
    }

    @Test
    void processMessage_ShouldProcessAndSaveMessage_WhenValid() {
        // Given
        MessageRequest request = new MessageRequest();
        request.setChatId(chatId);
        request.setEncryptedContent(encryptedContent);

        Message savedMessage = new Message();
        savedMessage.setId(UUID.randomUUID());
        savedMessage.setChatId(chatId);
        savedMessage.setSenderId(senderId);
        savedMessage.setEncryptedContent(encryptedContent);
        savedMessage.setContentHash(calculateExpectedHash(encryptedContent));
        savedMessage.setSentAt(LocalDateTime.now());
        savedMessage.setIsDeleted(false);

        when(participantRepository.existsByChatIdAndUserId(chatId, senderId)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        // When
        MessageResponse response = messageService.processMessage(request, senderId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getChatId()).isEqualTo(chatId);
        assertThat(response.getSenderId()).isEqualTo(senderId);
        assertThat(response.getEncryptedContent()).isEqualTo(encryptedContent);
        assertThat(response.getContentHash()).isNotNull();
        assertThat(response.getContentHash()).hasSize(64);

        // Исправленная проверка - используем ArgumentCaptor
        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<WebSocketMessage> messageCaptor = ArgumentCaptor.forClass(WebSocketMessage.class);

        verify(messagingTemplate, times(1)).convertAndSend(
                destinationCaptor.capture(),
                messageCaptor.capture()
        );

        assertThat(destinationCaptor.getValue()).isEqualTo("/topic/chat/" + chatId);
        assertThat(messageCaptor.getValue().getChatId()).isEqualTo(chatId);
        assertThat(messageCaptor.getValue().getSenderId()).isEqualTo(senderId);
        assertThat(messageCaptor.getValue().getEncryptedContent()).isEqualTo(encryptedContent);
        assertThat(messageCaptor.getValue().getContentHash()).isEqualTo(calculateExpectedHash(encryptedContent));

        verify(kafkaProducerService, times(1)).sendMessageEvent(any(KafkaMessageEvent.class));
    }

    @Test
    void processMessage_ShouldThrowException_WhenUserNotParticipant() {
        // Given
        MessageRequest request = new MessageRequest();
        request.setChatId(chatId);
        request.setEncryptedContent(encryptedContent);

        when(participantRepository.existsByChatIdAndUserId(chatId, senderId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> messageService.processMessage(request, senderId))
                .isInstanceOf(ChatException.class)
                .hasMessage("User is not a participant of this chat");

        verify(messageRepository, never()).save(any(Message.class));
        verify(messagingTemplate, never()).convertAndSend(anyString(), Optional.ofNullable(any()));
        verify(kafkaProducerService, never()).sendMessageEvent(any());
    }

    @Test
    void processMessage_ShouldGenerateCorrectHash() {
        // Given
        MessageRequest request = new MessageRequest();
        request.setChatId(chatId);
        request.setEncryptedContent(encryptedContent);

        when(participantRepository.existsByChatIdAndUserId(chatId, senderId)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            message.setId(UUID.randomUUID());
            message.setSentAt(LocalDateTime.now());
            return message;
        });

        // When
        MessageResponse response = messageService.processMessage(request, senderId);

        // Then
        assertThat(response.getContentHash()).isEqualTo(calculateExpectedHash(encryptedContent));

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getContentHash()).isEqualTo(calculateExpectedHash(encryptedContent));
    }

    @Test
    void processMessage_ShouldSendKafkaEventWithCorrectData() {
        // Given
        MessageRequest request = new MessageRequest();
        request.setChatId(chatId);
        request.setEncryptedContent(encryptedContent);

        Message savedMessage = new Message();
        savedMessage.setId(UUID.randomUUID());
        savedMessage.setChatId(chatId);
        savedMessage.setSenderId(senderId);
        savedMessage.setEncryptedContent(encryptedContent);
        savedMessage.setContentHash(calculateExpectedHash(encryptedContent));
        savedMessage.setSentAt(LocalDateTime.now());
        savedMessage.setIsDeleted(false);

        when(participantRepository.existsByChatIdAndUserId(chatId, senderId)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        // When
        messageService.processMessage(request, senderId);

        // Then
        ArgumentCaptor<KafkaMessageEvent> eventCaptor = ArgumentCaptor.forClass(KafkaMessageEvent.class);
        verify(kafkaProducerService, times(1)).sendMessageEvent(eventCaptor.capture());

        KafkaMessageEvent event = eventCaptor.getValue();
        assertThat(event.getMessageId()).isEqualTo(savedMessage.getId());
        assertThat(event.getChatId()).isEqualTo(chatId);
        assertThat(event.getSenderId()).isEqualTo(senderId);
        assertThat(event.getContentHash()).isEqualTo(calculateExpectedHash(encryptedContent));
        assertThat(event.getEncryptedContent()).isEqualTo(encryptedContent);
        assertThat(event.getTimestamp()).isNotNull();
    }

    private String calculateExpectedHash(String content) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "";
        }
    }
}