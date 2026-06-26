package com.messenger.chat.service;

import com.messenger.chat.dto.ChatRequest;
import com.messenger.chat.dto.ChatResponse;
import com.messenger.chat.entity.Chat;
import com.messenger.chat.entity.ChatParticipant;
import com.messenger.chat.exception.ChatException;
import com.messenger.chat.repository.ChatParticipantRepository;
import com.messenger.chat.repository.ChatRepository;
import com.messenger.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatParticipantRepository participantRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public ChatResponse createChat(ChatRequest request, UUID creatorId) {
        log.info("Creating chat: type={}, participants={}", request.getType(), request.getParticipantIds());

        // Проверяем, что участники указаны
        if (request.getParticipantIds() == null || request.getParticipantIds().isEmpty()) {
            throw new ChatException("At least one participant is required");
        }

        // Для диалога проверяем, что участников ровно 2
        Chat.ChatType type = Chat.ChatType.valueOf(request.getType());
        if (type == Chat.ChatType.DIALOG && request.getParticipantIds().size() != 2) {
            throw new ChatException("Dialog must have exactly 2 participants");
        }

        // Проверяем, что создатель включен в участники
        if (!request.getParticipantIds().contains(creatorId)) {
            throw new ChatException("Creator must be a participant");
        }

        // Создаем чат
        Chat chat = new Chat();
        chat.setName(request.getName());
        chat.setType(type);
        chat.setCreatedBy(creatorId);
        chat.setCreatedAt(LocalDateTime.now());

        Chat savedChat = chatRepository.save(chat);
        log.info("Chat created with ID: {}", savedChat.getId());

        // Добавляем участников
        request.getParticipantIds().forEach(userId -> {
            ChatParticipant participant = new ChatParticipant();
            participant.setChatId(savedChat.getId());
            participant.setUserId(userId);
            participant.setJoinedAt(LocalDateTime.now());
            participant.setRole(userId.equals(creatorId) ?
                    ChatParticipant.Role.ADMIN : ChatParticipant.Role.MEMBER);
            participantRepository.save(participant);
        });

        return mapToResponse(savedChat, creatorId);
    }

    @Transactional
    public void addParticipants(UUID chatId, List<UUID> userIds) {
        log.info("Adding participants to chat {}: {}", chatId, userIds);

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatException("Chat not found"));

        userIds.forEach(userId -> {
            if (!participantRepository.existsByChatIdAndUserId(chatId, userId)) {
                ChatParticipant participant = new ChatParticipant();
                participant.setChatId(chatId);
                participant.setUserId(userId);
                participant.setJoinedAt(LocalDateTime.now());
                participant.setRole(ChatParticipant.Role.MEMBER);
                participantRepository.save(participant);
            }
        });
    }

    @Transactional
    public void removeParticipant(UUID chatId, UUID userId) {
        log.info("Removing participant {} from chat {}", userId, chatId);

        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ChatException("Chat not found"));

        // Проверяем, что это не единственный администратор
        long adminCount = participantRepository.findByChatId(chatId).stream()
                .filter(p -> p.getRole() == ChatParticipant.Role.ADMIN)
                .count();

        if (adminCount <= 1) {
            throw new ChatException("Cannot remove last admin");
        }

        participantRepository.deleteByChatIdAndUserId(chatId, userId);
    }

    @Transactional(readOnly = true)
    public List<ChatResponse> getUserChats(UUID userId) {
        log.info("Getting chats for user: {}", userId);

        List<Chat> chats = chatRepository.findChatsByUserId(userId);
        return chats.stream()
                .map(chat -> mapToResponse(chat, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatResponse.MessageInfo> getChatMessages(UUID chatId, UUID userId, int page, int size) {
        log.info("Getting messages for chat {} from user {}, page={}, size={}", chatId, userId, page, size);

        // Проверяем, что пользователь участник чата
        if (!participantRepository.existsByChatIdAndUserId(chatId, userId)) {
            throw new ChatException("User is not a participant of this chat");
        }

        Pageable pageable = PageRequest.of(page, size);
        return messageRepository.findByChatIdOrderBySentAtDesc(chatId, pageable)
                .stream()
                .map(msg -> ChatResponse.MessageInfo.builder()
                        .id(msg.getId())
                        .senderId(msg.getSenderId())
                        .contentHash(msg.getContentHash())
                        .sentAt(msg.getSentAt())
                        .build())
                .collect(Collectors.toList());
    }

    private ChatResponse mapToResponse(Chat chat, UUID userId) {
        List<ChatResponse.ParticipantInfo> participants = participantRepository.findByChatId(chat.getId())
                .stream()
                .map(p -> ChatResponse.ParticipantInfo.builder()
                        .userId(p.getUserId())
                        .role(p.getRole().name())
                        .joinedAt(p.getJoinedAt())
                        .build())
                .collect(Collectors.toList());

        return ChatResponse.builder()
                .id(chat.getId())
                .name(chat.getName())
                .type(chat.getType())
                .createdBy(chat.getCreatedBy())
                .createdAt(chat.getCreatedAt())
                .participants(participants)
                .build();
    }
}