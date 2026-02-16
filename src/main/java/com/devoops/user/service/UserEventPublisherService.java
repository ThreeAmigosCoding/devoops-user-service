package com.devoops.user.service;

import com.devoops.user.dto.message.UserCreatedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventPublisherService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.notification}")
    private String notificationExchange;

    @Value("${rabbitmq.routing-key.user-created}")
    private String userCreatedRoutingKey;

    public void publishUserCreated(UUID userId, String email) {
        UserCreatedMessage message = UserCreatedMessage.builder()
                .userId(userId)
                .userEmail(email)
                .build();

        log.info("Publishing user.created event for userId: {}, email: {}", userId, email);
        rabbitTemplate.convertAndSend(notificationExchange, userCreatedRoutingKey, message);
        log.debug("Successfully published user.created event for userId: {}", userId);
    }
}
