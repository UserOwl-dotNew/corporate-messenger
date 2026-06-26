package com.messenger.chat.service;

import com.messenger.chat.dto.ChatRequest;
import com.messenger.chat.dto.ChatResponse;
import com.messenger.chat.entity.Chat;
import com.messenger.chat.entity.ChatParticipant;
import com.messenger.chat.exception.ChatException;
import com.messenger.chat.repository.ChatParticipantRepository;
import com.messenger.chat.repository.ChatRepository;
import com.messenger.chat.repository.MessageRepository;
import com.messenger.chat.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private ChatParticipantRepository participantRepository;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private ChatService chatService;

    private UUID creatorId;
    private UUID userId1;
    private UUID userId2;
    private UUID chatId;

    @BeforeEach
    void setUp() {
        creatorId = UUID.randomUUID();
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        chatId = UUID.randomUUID();
    }

    @Test
    void createChat_ShouldCreateGroupChat_WhenValidRequest() {
        // Given
        ChatRequest request = new ChatRequest();
        request.setName("Test Group");
        request.setType("GROUP");
        request.setParticipantIds(List.of(creatorId, userId1, userId2));

        Chat savedChat = new Chat();
        savedChat.setId(chatId);
        savedChat.setName(request.getName());
        savedChat.setType(Chat.ChatType.GROUP);
        savedChat.setCreatedBy(creatorId);
        savedChat.setCreatedAt(LocalDateTime.now());

        when(chatRepository.save(any(Chat.class))).thenReturn(savedChat);
        when(participantRepository.save(any(ChatParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ChatResponse response = chatService.createChat(request, creatorId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(chatId);
        assertThat(response.getName()).isEqualTo("Test Group");
        assertThat(response.getType()).isEqualTo(Chat.ChatType.GROUP);
        assertThat(response.getCreatedBy()).isEqualTo(creatorId);

        ArgumentCaptor<Chat> chatCaptor = ArgumentCaptor.forClass(Chat.class);
        verify(chatRepository).save(chatCaptor.capture());
        assertThat(chatCaptor.getValue().getName()).isEqualTo("Test Group");

        verify(participantRepository, times(3)).save(any(ChatParticipant.class));
    }

    @Test
    void createChat_ShouldCreateDialog_WhenValidRequest() {
        // Given
        ChatRequest request = new ChatRequest();
        request.setType("DIALOG");
        request.setParticipantIds(List.of(creatorId, userId1));

        Chat savedChat = new Chat();
        savedChat.setId(chatId);
        savedChat.setType(Chat.ChatType.DIALOG);
        savedChat.setCreatedBy(creatorId);
        savedChat.setCreatedAt(LocalDateTime.now());

        when(chatRepository.save(any(Chat.class))).thenReturn(savedChat);
        when(participantRepository.save(any(ChatParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ChatResponse response = chatService.createChat(request, creatorId);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getType()).isEqualTo(Chat.ChatType.DIALOG);
        verify(participantRepository, times(2)).save(any(ChatParticipant.class));
    }

    @Test
    void createChat_ShouldThrowException_WhenNoParticipants() {
        // Given
        ChatRequest request = new ChatRequest();
        request.setType("GROUP");
        request.setParticipantIds(List.of());

        // When & Then
        assertThatThrownBy(() -> chatService.createChat(request, creatorId))
                .isInstanceOf(ChatException.class)
                .hasMessage("At least one participant is required");
    }

    @Test
    void createChat_ShouldThrowException_WhenCreatorNotParticipant() {
        // Given
        ChatRequest request = new ChatRequest();
        request.setType("GROUP");
        request.setParticipantIds(List.of(userId1, userId2));

        // When & Then
        assertThatThrownBy(() -> chatService.createChat(request, creatorId))
                .isInstanceOf(ChatException.class)
                .hasMessage("Creator must be a participant");
    }

    @Test
    void createDialog_ShouldThrowException_WhenMoreThan2Participants() {
        // Given
        ChatRequest request = new ChatRequest();
        request.setType("DIALOG");
        request.setParticipantIds(List.of(creatorId, userId1, userId2));

        // When & Then
        assertThatThrownBy(() -> chatService.createChat(request, creatorId))
                .isInstanceOf(ChatException.class)
                .hasMessage("Dialog must have exactly 2 participants");
    }

    @Test
    void addParticipants_ShouldAddNewParticipants_WhenValid() {
        // Given
        Chat chat = new Chat();
        chat.setId(chatId);
        chat.setType(Chat.ChatType.GROUP);

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(participantRepository.existsByChatIdAndUserId(chatId, userId1)).thenReturn(false);
        when(participantRepository.existsByChatIdAndUserId(chatId, userId2)).thenReturn(false);
        when(participantRepository.save(any(ChatParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        chatService.addParticipants(chatId, List.of(userId1, userId2));

        // Then
        verify(participantRepository, times(2)).save(any(ChatParticipant.class));
    }

    @Test
    void addParticipants_ShouldNotAddExistingParticipants() {
        // Given
        Chat chat = new Chat();
        chat.setId(chatId);

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(participantRepository.existsByChatIdAndUserId(chatId, userId1)).thenReturn(true);
        when(participantRepository.existsByChatIdAndUserId(chatId, userId2)).thenReturn(false);
        when(participantRepository.save(any(ChatParticipant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        chatService.addParticipants(chatId, List.of(userId1, userId2));

        // Then
        verify(participantRepository, times(1)).save(any(ChatParticipant.class));
    }

    @Test
    void addParticipants_ShouldThrowException_WhenChatNotFound() {
        // Given
        when(chatRepository.findById(chatId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatService.addParticipants(chatId, List.of(userId1)))
                .isInstanceOf(ChatException.class)
                .hasMessage("Chat not found");
    }

    @Test
    void removeParticipant_ShouldRemove_WhenValid() {
        // Given
        Chat chat = new Chat();
        chat.setId(chatId);

        ChatParticipant admin1 = new ChatParticipant();
        admin1.setUserId(creatorId);
        admin1.setRole(ChatParticipant.Role.ADMIN);
        admin1.setChatId(chatId);

        ChatParticipant admin2 = new ChatParticipant();  // <-- Добавить второго админа
        admin2.setUserId(UUID.randomUUID());
        admin2.setRole(ChatParticipant.Role.ADMIN);
        admin2.setChatId(chatId);

        ChatParticipant member = new ChatParticipant();
        member.setUserId(userId1);
        member.setRole(ChatParticipant.Role.MEMBER);
        member.setChatId(chatId);

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(participantRepository.findByChatId(chatId)).thenReturn(List.of(admin1, admin2, member));  // <-- Добавить второго админа

        // When
        chatService.removeParticipant(chatId, userId1);

        // Then
        verify(participantRepository).deleteByChatIdAndUserId(chatId, userId1);
    }

    @Test
    void removeParticipant_ShouldThrowException_WhenLastAdmin() {
        // Given
        Chat chat = new Chat();
        chat.setId(chatId);

        ChatParticipant admin = new ChatParticipant();
        admin.setUserId(creatorId);
        admin.setRole(ChatParticipant.Role.ADMIN);
        admin.setChatId(chatId);

        when(chatRepository.findById(chatId)).thenReturn(Optional.of(chat));
        when(participantRepository.findByChatId(chatId)).thenReturn(List.of(admin));

        // When & Then
        assertThatThrownBy(() -> chatService.removeParticipant(chatId, creatorId))
                .isInstanceOf(ChatException.class)
                .hasMessage("Cannot remove last admin");
    }

    @Test
    void getUserChats_ShouldReturnChats_WhenUserExists() {
        // Given
        Chat chat1 = new Chat();
        chat1.setId(UUID.randomUUID());
        chat1.setName("Chat 1");
        chat1.setType(Chat.ChatType.GROUP);
        chat1.setCreatedAt(LocalDateTime.now());

        Chat chat2 = new Chat();
        chat2.setId(UUID.randomUUID());
        chat2.setName("Chat 2");
        chat2.setType(Chat.ChatType.DIALOG);
        chat2.setCreatedAt(LocalDateTime.now());

        List<Chat> chats = List.of(chat1, chat2);

        when(chatRepository.findChatsByUserId(creatorId)).thenReturn(chats);

        // When
        List<ChatResponse> responses = chatService.getUserChats(creatorId);

        // Then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getName()).isEqualTo("Chat 1");
        assertThat(responses.get(1).getName()).isEqualTo("Chat 2");
    }
}