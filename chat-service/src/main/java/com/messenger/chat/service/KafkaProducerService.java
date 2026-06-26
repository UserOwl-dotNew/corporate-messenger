package com.messenger.chat.service;

import com.messenger.chat.dto.KafkaMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private static final String MESSAGES_TOPIC = "chat-messages";
    private static final String SEARCH_TOPIC = "search-index";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void sendMessageEvent(KafkaMessageEvent event) {
        try {
            log.info("Sending message event to Kafka: {}", event.getMessageId());
            kafkaTemplate.send(MESSAGES_TOPIC, event.getMessageId().toString(), event);
            log.info("Message event sent successfully to topic: {}", MESSAGES_TOPIC);
        } catch (Exception e) {
            log.error("Failed to send message event to Kafka", e);
        }
    }

    @Async
    public void sendSearchEvent(KafkaMessageEvent event) {
        try {
            log.info("Sending search event to Kafka: {}", event.getMessageId());
            kafkaTemplate.send(SEARCH_TOPIC, event.getMessageId().toString(), event);
            log.info("Search event sent successfully to topic: {}", SEARCH_TOPIC);
        } catch (Exception e) {
            log.error("Failed to send search event to Kafka", e);
        }
    }
}