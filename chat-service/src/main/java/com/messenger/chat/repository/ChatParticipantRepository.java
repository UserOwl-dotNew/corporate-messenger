package com.messenger.chat.repository;

import com.messenger.chat.entity.ChatParticipant;
import com.messenger.chat.entity.ChatParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, ChatParticipantId> {

    List<ChatParticipant> findByChatId(UUID chatId);

    List<ChatParticipant> findByUserId(UUID userId);

    boolean existsByChatIdAndUserId(UUID chatId, UUID userId);

    void deleteByChatIdAndUserId(UUID chatId, UUID userId);
}