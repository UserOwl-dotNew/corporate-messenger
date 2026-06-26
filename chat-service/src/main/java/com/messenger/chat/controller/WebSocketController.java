package com.messenger.chat.controller;

import com.messenger.chat.dto.MessageRequest;
import com.messenger.chat.dto.MessageResponse;
import com.messenger.chat.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final MessageService messageService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload MessageRequest request, SimpMessageHeaderAccessor headerAccessor) {
        log.info("WebSocket message received: chatId={}", request.getChatId());

        // В реальном проекте userId извлекается из JWT токена в WebSocket сессии
        UUID senderId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        MessageResponse response = messageService.processMessage(request, senderId);
        log.info("WebSocket message processed: messageId={}", response.getId());
    }
}