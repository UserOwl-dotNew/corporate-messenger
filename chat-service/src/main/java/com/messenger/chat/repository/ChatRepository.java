package com.messenger.chat.repository;

import com.messenger.chat.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<Chat, UUID> {

    @Query("SELECT c FROM Chat c JOIN c.participants p WHERE p.userId = :userId")
    List<Chat> findChatsByUserId(@Param("userId") UUID userId);

    @Query("SELECT c FROM Chat c JOIN c.participants p WHERE c.type = 'DIALOG' AND p.userId IN (:userId1, :userId2) " +
            "GROUP BY c HAVING COUNT(p) = 2")
    List<Chat> findDialogBetweenUsers(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);
}