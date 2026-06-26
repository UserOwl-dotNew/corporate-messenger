package com.messenger.chat.controller;

import com.messenger.chat.dto.ChatRequest;
import com.messenger.chat.dto.ChatResponse;
import com.messenger.chat.dto.ParticipantRequest;
import com.messenger.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // В реальном проекте userId берется из SecurityContext
    private UUID getCurrentUserId() {
        return UUID.fromString("11111111-1111-1111-1111-111111111111");
    }

    @PostMapping
    public ResponseEntity<ChatResponse> createChat(@Valid @RequestBody ChatRequest request) {
        UUID userId = getCurrentUserId();
        ChatResponse response = chatService.createChat(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{chatId}/participants")
    public ResponseEntity<Void> addParticipants(
            @PathVariable UUID chatId,
            @Valid @RequestBody ParticipantRequest request) {
        chatService.addParticipants(chatId, request.getUserIds());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{chatId}/participants/{userId}")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable UUID chatId,
            @PathVariable UUID userId) {
        chatService.removeParticipant(chatId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<List<ChatResponse>> getUserChats() {
        UUID userId = getCurrentUserId();
        List<ChatResponse> chats = chatService.getUserChats(userId);
        return ResponseEntity.ok(chats);
    }

    @GetMapping("/{chatId}/messages")
    public ResponseEntity<List<ChatResponse.MessageInfo>> getChatMessages(
            @PathVariable UUID chatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = getCurrentUserId();
        List<ChatResponse.MessageInfo> messages = chatService.getChatMessages(chatId, userId, page, size);
        return ResponseEntity.ok(messages);
    }
}