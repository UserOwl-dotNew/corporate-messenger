package com.messenger.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messenger.ChatApplication;
import com.messenger.chat.dto.ChatRequest;
import com.messenger.chat.entity.Chat;
import com.messenger.chat.entity.ChatParticipant;
import com.messenger.chat.repository.ChatParticipantRepository;
import com.messenger.chat.repository.ChatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = ChatApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChatControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private ChatParticipantRepository participantRepository;

    private UUID creatorId;
    private UUID userId1;
    private UUID userId2;

    @BeforeEach
    void setUp() {
        creatorId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
    }

    @Test
    void createChat_ShouldReturnCreatedChat() throws Exception {
        // Given
        ChatRequest request = new ChatRequest();
        request.setName("Integration Test Chat");
        request.setType("GROUP");
        request.setParticipantIds(List.of(creatorId, userId1, userId2));

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/chats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        // Then
        String jsonResponse = result.getResponse().getContentAsString();
        assertThat(jsonResponse).contains("Integration Test Chat");
        assertThat(jsonResponse).contains("GROUP");
    }

    @Test
    void createChat_ShouldReturnBadRequest_WhenNoParticipants() throws Exception {
        // Given
        ChatRequest request = new ChatRequest();
        request.setType("GROUP");
        request.setParticipantIds(List.of());

        // When & Then
        mockMvc.perform(post("/api/v1/chats")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserChats_ShouldReturnChats() throws Exception {
        // Given
        Chat chat = new Chat();
        chat.setName("Test Chat");
        chat.setType(Chat.ChatType.GROUP);
        chat.setCreatedBy(creatorId);
        chat.setCreatedAt(LocalDateTime.now());
        Chat savedChat = chatRepository.save(chat);

        ChatParticipant participant = new ChatParticipant();
        participant.setChatId(savedChat.getId());
        participant.setUserId(creatorId);
        participant.setJoinedAt(LocalDateTime.now());
        participant.setRole(ChatParticipant.Role.ADMIN);
        participantRepository.save(participant);

        // When
        MvcResult result = mockMvc.perform(get("/api/v1/chats/me"))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        String jsonResponse = result.getResponse().getContentAsString();
        assertThat(jsonResponse).contains("Test Chat");
    }

    @Test
    void addParticipants_ShouldAddToChat() throws Exception {
        // Given
        Chat chat = new Chat();
        chat.setName("Test Chat");
        chat.setType(Chat.ChatType.GROUP);
        chat.setCreatedBy(creatorId);
        chat.setCreatedAt(LocalDateTime.now());
        Chat savedChat = chatRepository.save(chat);

        // When
        mockMvc.perform(post("/api/v1/chats/{chatId}/participants", savedChat.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userIds\": [\"" + userId1 + "\", \"" + userId2 + "\"]}"))
                .andExpect(status().isOk());

        // Then
        List<ChatParticipant> participants = participantRepository.findByChatId(savedChat.getId());
        assertThat(participants).hasSize(2);
    }
}