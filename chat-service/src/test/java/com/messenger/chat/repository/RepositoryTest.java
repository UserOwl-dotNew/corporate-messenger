package com.messenger.chat.repository;

import com.messenger.ChatApplication;
import com.messenger.chat.entity.Chat;
import com.messenger.chat.entity.ChatParticipant;
import com.messenger.chat.entity.Message;
import com.messenger.chat.repository.ChatParticipantRepository;
import com.messenger.chat.repository.ChatRepository;
import com.messenger.chat.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryTest {

    @Autowired
    private ChatRepository chatRepository;

    @Autowired
    private ChatParticipantRepository participantRepository;

    @Autowired
    private MessageRepository messageRepository;

    private UUID userId1;
    private UUID userId2;
    private UUID userId3;
    private Chat chat;

    @BeforeEach
    void setUp() {
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        userId3 = UUID.randomUUID();

        // Create chat
        chat = new Chat();
        chat.setName("Test Chat");
        chat.setType(Chat.ChatType.GROUP);
        chat.setCreatedBy(userId1);
        chat.setCreatedAt(LocalDateTime.now());
        chat = chatRepository.save(chat);

        // Add participants
        addParticipant(userId1, ChatParticipant.Role.ADMIN);
        addParticipant(userId2, ChatParticipant.Role.MEMBER);
        addParticipant(userId3, ChatParticipant.Role.MEMBER);
    }

    private void addParticipant(UUID userId, ChatParticipant.Role role) {
        ChatParticipant participant = new ChatParticipant();
        participant.setChatId(chat.getId());
        participant.setUserId(userId);
        participant.setJoinedAt(LocalDateTime.now());
        participant.setRole(role);
        participantRepository.save(participant);
    }

    @Test
    void chatRepository_FindChatsByUserId_ShouldReturnUserChats() {
        // When
        List<Chat> chats = chatRepository.findChatsByUserId(userId1);

        // Then
        assertThat(chats).hasSize(1);
        assertThat(chats.get(0).getId()).isEqualTo(chat.getId());
    }

    @Test
    void chatRepository_FindDialogBetweenUsers_ShouldReturnDialog() {
        // Given - create dialog
        Chat dialog = new Chat();
        dialog.setType(Chat.ChatType.DIALOG);
        dialog.setCreatedBy(userId1);
        dialog.setCreatedAt(LocalDateTime.now());
        dialog = chatRepository.save(dialog);

        addParticipantToDialog(dialog.getId(), userId1);
        addParticipantToDialog(dialog.getId(), userId2);

        // When
        List<Chat> dialogs = chatRepository.findDialogBetweenUsers(userId1, userId2);

        // Then
        assertThat(dialogs).hasSize(1);
        assertThat(dialogs.get(0).getId()).isEqualTo(dialog.getId());
    }

    private void addParticipantToDialog(UUID chatId, UUID userId) {
        ChatParticipant participant = new ChatParticipant();
        participant.setChatId(chatId);
        participant.setUserId(userId);
        participant.setJoinedAt(LocalDateTime.now());
        participant.setRole(ChatParticipant.Role.MEMBER);
        participantRepository.save(participant);
    }

    @Test
    void participantRepository_ExistsByChatIdAndUserId_ShouldReturnTrue_WhenExists() {
        // When
        boolean exists = participantRepository.existsByChatIdAndUserId(chat.getId(), userId1);

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void participantRepository_ExistsByChatIdAndUserId_ShouldReturnFalse_WhenNotExists() {
        // When
        boolean exists = participantRepository.existsByChatIdAndUserId(chat.getId(), UUID.randomUUID());

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void participantRepository_FindByChatId_ShouldReturnAllParticipants() {
        // When
        List<ChatParticipant> participants = participantRepository.findByChatId(chat.getId());

        // Then
        assertThat(participants).hasSize(3);
        assertThat(participants).extracting("userId")
                .containsExactlyInAnyOrder(userId1, userId2, userId3);
    }

    @Test
    void participantRepository_DeleteByChatIdAndUserId_ShouldRemoveParticipant() {
        // When
        participantRepository.deleteByChatIdAndUserId(chat.getId(), userId2);

        // Then
        List<ChatParticipant> participants = participantRepository.findByChatId(chat.getId());
        assertThat(participants).hasSize(2);
        assertThat(participants).extracting("userId")
                .containsExactlyInAnyOrder(userId1, userId3);
    }

    @Test
    void messageRepository_SaveAndFindMessages_ShouldWork() {
        // Given
        Message message = new Message();
        message.setChatId(chat.getId());
        message.setSenderId(userId1);
        message.setEncryptedContent("encrypted-content");
        message.setContentHash("hash-123");
        message.setSentAt(LocalDateTime.now());
        message.setIsDeleted(false);
        message = messageRepository.save(message);

        // When
        Pageable pageable = PageRequest.of(0, 10);
        List<Message> messages = messageRepository.findByChatIdOrderBySentAtDesc(chat.getId(), pageable);

        // Then
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getId()).isEqualTo(message.getId());
        assertThat(messages.get(0).getEncryptedContent()).isEqualTo("encrypted-content");
        assertThat(messages.get(0).getContentHash()).isEqualTo("hash-123");
    }

    @Test
    void messageRepository_FindByBlockchainTxIdIsNullAndChatId_ShouldReturnPendingMessages() {
        // Given
        Message message1 = new Message();
        message1.setChatId(chat.getId());
        message1.setSenderId(userId1);
        message1.setEncryptedContent("content-1");
        message1.setContentHash("hash-1");
        message1.setSentAt(LocalDateTime.now());
        message1.setIsDeleted(false);
        message1.setBlockchainTxId(null);
        messageRepository.save(message1);

        Message message2 = new Message();
        message2.setChatId(chat.getId());
        message2.setSenderId(userId2);
        message2.setEncryptedContent("content-2");
        message2.setContentHash("hash-2");
        message2.setSentAt(LocalDateTime.now());
        message2.setIsDeleted(false);
        message2.setBlockchainTxId("tx-123");
        messageRepository.save(message2);

        // When
        List<Message> pendingMessages = messageRepository.findByBlockchainTxIdIsNullAndChatId(chat.getId());

        // Then
        assertThat(pendingMessages).hasSize(1);
        assertThat(pendingMessages.get(0).getEncryptedContent()).isEqualTo("content-1");
        assertThat(pendingMessages.get(0).getBlockchainTxId()).isNull();
    }
}