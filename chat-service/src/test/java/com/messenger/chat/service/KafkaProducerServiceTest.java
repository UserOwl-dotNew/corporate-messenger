package com.messenger.chat.service;

import com.messenger.chat.dto.KafkaMessageEvent;
import com.messenger.chat.service.KafkaProducerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private KafkaProducerService kafkaProducerService;

    private KafkaMessageEvent event;
    private UUID messageId;

    @BeforeEach
    void setUp() {
        messageId = UUID.randomUUID();
        event = KafkaMessageEvent.builder()
                .messageId(messageId)
                .chatId(UUID.randomUUID())
                .senderId(UUID.randomUUID())
                .contentHash("test-hash")
                .encryptedContent("test-content")
                .timestamp(System.currentTimeMillis())
                .build();
    }

    @Test
    void sendMessageEvent_ShouldSendToKafka() {
        // Given
        when(kafkaTemplate.send(eq("chat-messages"), eq(messageId.toString()), eq(event)))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When
        kafkaProducerService.sendMessageEvent(event);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);

        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("chat-messages");
        assertThat(keyCaptor.getValue()).isEqualTo(messageId.toString());
        assertThat(valueCaptor.getValue()).isEqualTo(event);
    }

    @Test
    void sendSearchEvent_ShouldSendToSearchTopic() {
        // Given
        when(kafkaTemplate.send(eq("search-index"), eq(messageId.toString()), eq(event)))
                .thenReturn(CompletableFuture.completedFuture(null));

        // When
        kafkaProducerService.sendSearchEvent(event);

        // Then
        verify(kafkaTemplate).send(eq("search-index"), eq(messageId.toString()), eq(event));
    }

    @Test
    void sendMessageEvent_ShouldHandleKafkaExceptionGracefully() {
        // Given
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("Kafka connection failed"));

        // When & Then - should not throw exception
        kafkaProducerService.sendMessageEvent(event);

        // Verify still called
        verify(kafkaTemplate).send(anyString(), anyString(), any());
    }
}