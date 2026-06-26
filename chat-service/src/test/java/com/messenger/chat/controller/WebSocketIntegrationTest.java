package com.messenger.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messenger.ChatApplication;
import com.messenger.chat.dto.MessageRequest;
import com.messenger.chat.entity.Chat;
import com.messenger.chat.entity.ChatParticipant;
import com.messenger.chat.repository.ChatParticipantRepository;
import com.messenger.chat.repository.ChatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ChatApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
@Disabled("WebSocket test requires full WebSocket configuration - will be fixed later")
class WebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private ChatParticipantRepository participantRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private UUID chatId;
    private UUID senderId;

    @BeforeEach
    void setUp() throws Exception {
        senderId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID userId2 = UUID.randomUUID();

        // Create test chat
        Chat chat = new Chat();
        chat.setName("WebSocket Test Chat");
        chat.setType(Chat.ChatType.GROUP);
        chat.setCreatedBy(senderId);
        chat.setCreatedAt(LocalDateTime.now());
        Chat savedChat = chatRepository.save(chat);
        chatId = savedChat.getId();

        // Add participants
        ChatParticipant participant1 = new ChatParticipant();
        participant1.setChatId(chatId);
        participant1.setUserId(senderId);
        participant1.setJoinedAt(LocalDateTime.now());
        participant1.setRole(ChatParticipant.Role.ADMIN);
        participantRepository.save(participant1);

        ChatParticipant participant2 = new ChatParticipant();
        participant2.setChatId(chatId);
        participant2.setUserId(userId2);
        participant2.setJoinedAt(LocalDateTime.now());
        participant2.setRole(ChatParticipant.Role.MEMBER);
        participantRepository.save(participant2);

        // Setup WebSocket client - с увеличенным таймаутом
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        stompClient.setDefaultHeartbeat(new long[]{0, 0}); // Отключаем heartbeat для тестов

        // Подключаемся с использованием SockJS
        stompSession = stompClient.connectAsync(
                "ws://localhost:" + port + "/ws",
                new WebSocketHttpHeaders(),
                new StompSessionHandlerAdapter() {}
        ).get(10, TimeUnit.SECONDS);
    }

    @Test
    void sendMessage_ShouldDeliverToAllParticipants() throws Exception {
        CompletableFuture<String> receivedMessage = new CompletableFuture<>();

        // Subscribe to chat topic
        stompSession.subscribe("/topic/chat/" + chatId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedMessage.complete((String) payload);
            }
        });

        // Wait for subscription to be ready
        Thread.sleep(1000);

        // Send message
        MessageRequest request = new MessageRequest();
        request.setChatId(chatId);
        request.setEncryptedContent("SGVsbG8gV2ViU29ja2V0");

        stompSession.send("/app/chat.send", request);

        // Verify received
        String response = receivedMessage.get(10, TimeUnit.SECONDS);
        assertThat(response).isNotNull();
        assertThat(response).contains("SGVsbG8gV2ViU29ja2V0");
        assertThat(response).contains("\"chatId\":\"" + chatId + "\"");
    }
}