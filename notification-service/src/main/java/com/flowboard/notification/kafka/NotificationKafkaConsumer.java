package com.flowboard.notification.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowboard.notification.entity.Notification;
import com.flowboard.notification.repository.NotificationRepository;
import com.flowboard.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaConsumer {
    private static final String EMAIL_FIELD = "email";
    private static final String FULL_NAME_FIELD = "fullName";
    private static final String USERS_SEGMENT = "users";
    private static final String USERNAME_SEGMENT = "username";

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${auth.service.internal-base-url:http://localhost:8081/api/v1/auth/internal}")
    private String authServiceUrl;

    @Value("${card.service.base-url:http://localhost:8085/api/v1/cards}")
    private String cardServiceUrl;

    @KafkaListener(topics = "flowboard.mention.notification", groupId = "notification-group")
    public void handleMention(String rawPayload) {
        Map<String, Object> payload = parsePayload(rawPayload);
        log.info("Received mention event: {}", payload);
        try {
            String username = (String) payload.get("username");
            String actorId = payload.get("actorId").toString();
            String cardId = payload.get("cardId").toString();
            String cardName = resolveCardName(cardId);

            Map<String, Object> user = restTemplate.getForObject(buildAuthServiceUri(USERS_SEGMENT, USERNAME_SEGMENT, username), Map.class);
            if (user != null) {
                Long userId = Long.valueOf(user.get("id").toString());
                String email = (String) user.get(EMAIL_FIELD);
                String actorName = resolveUserName(Long.valueOf(actorId));

                saveNotification(userId, "New Mention", actorName + " mentioned you in " + cardName, Notification.NotificationType.MENTION);
                emailService.sendMentionNotification(email, actorName, cardName);
            }
        } catch (Exception e) {
            log.error("Failed to process mention event", e);
        }
    }

    @KafkaListener(topics = "flowboard.card.assigned", groupId = "notification-group")
    public void handleAssignment(String rawPayload) {
        Map<String, Object> payload = parsePayload(rawPayload);
        log.info("Received card assignment event: {}", payload);
        try {
            Long assigneeId = Long.valueOf(payload.get("assigneeId").toString());
            String cardId = payload.get("cardId").toString();
            String cardName = resolveCardName(cardId);

            Map<String, Object> user = restTemplate.getForObject(buildAuthServiceUri(USERS_SEGMENT, assigneeId.toString()), Map.class);
            if (user != null) {
                String email = (String) user.get(EMAIL_FIELD);

                saveNotification(assigneeId, "New Assignment", "You have been assigned to " + cardName, Notification.NotificationType.ASSIGNMENT);
                emailService.sendAssignmentNotification(email, cardName);
            }
        } catch (Exception e) {
            log.error("Failed to process assignment event", e);
        }
    }

    @KafkaListener(topics = "flowboard.workspace.member.invited", groupId = "notification-group")
    public void handleWorkspaceInvite(String rawPayload) {
        Map<String, Object> payload = parsePayload(rawPayload);
        log.info("Received workspace invitation event: {}", payload);
        try {
            Long invitedUserId = Long.valueOf(payload.get("invitedUserId").toString());
            Long invitedByUserId = Long.valueOf(payload.get("invitedByUserId").toString());
            String workspaceName = (String) payload.get("workspaceName");
            String role = (String) payload.get("role");

            Map<String, Object> invitedUser = restTemplate.getForObject(buildAuthServiceUri(USERS_SEGMENT, invitedUserId.toString()), Map.class);
            String inviterName = resolveUserName(invitedByUserId);

            if (invitedUser != null) {
                String email = (String) invitedUser.get(EMAIL_FIELD);

                saveNotification(invitedUserId, "Workspace Invitation",
                        inviterName + " invited you to workspace \"" + workspaceName + "\" as " + role, Notification.NotificationType.BROADCAST);
                emailService.sendWorkspaceInvitation(email, workspaceName, inviterName, role);
            }
        } catch (Exception e) {
            log.error("Failed to process workspace invitation event", e);
        }
    }

    @KafkaListener(topics = "flowboard.board.member.invited", groupId = "notification-group")
    public void handleBoardInvite(String rawPayload) {
        Map<String, Object> payload = parsePayload(rawPayload);
        log.info("Received board invitation event: {}", payload);
        try {
            Long invitedUserId = Long.valueOf(payload.get("invitedUserId").toString());
            Long invitedByUserId = Long.valueOf(payload.get("invitedByUserId").toString());
            String boardName = (String) payload.get("boardName");
            String role = (String) payload.get("role");

            Map<String, Object> invitedUser = restTemplate.getForObject(buildAuthServiceUri(USERS_SEGMENT, invitedUserId.toString()), Map.class);
            String inviterName = resolveUserName(invitedByUserId);

            if (invitedUser != null) {
                String email = (String) invitedUser.get(EMAIL_FIELD);

                saveNotification(invitedUserId, "Board Invitation",
                        inviterName + " added you to board \"" + boardName + "\" as " + role, Notification.NotificationType.BROADCAST);
                emailService.sendBoardInvitation(email, boardName, inviterName, role);
            }
        } catch (Exception e) {
            log.error("Failed to process board invitation event", e);
        }
    }

    @KafkaListener(topics = "flowboard.payment.premium.activated", groupId = "notification-group")
    public void handlePremiumActivated(String rawPayload) {
        Map<String, Object> payload = parsePayload(rawPayload);
        log.info("Received premium activated event: {}", payload);
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            String planName = (String) payload.get("planName");
            Integer amountPaise = Integer.valueOf(payload.get("amountPaise").toString());
            String currency = (String) payload.get("currency");
            String providerName = (String) payload.get("providerName");
            String providerOrderId = (String) payload.get("providerOrderId");
            String providerPaymentId = (String) payload.get("providerPaymentId");
            LocalDateTime activatedAt = LocalDateTime.parse(payload.get("activatedAt").toString());

            Map<String, Object> user = restTemplate.getForObject(buildAuthServiceUri(USERS_SEGMENT, userId.toString()), Map.class);
            if (user != null) {
                String email = (String) user.get(EMAIL_FIELD);
                String fullName = user.get(FULL_NAME_FIELD) != null ? user.get(FULL_NAME_FIELD).toString() : "FlowBoard member";

                saveNotification(userId, "Premium activated",
                        "Your FlowBoard Premium plan is active and your receipt is on its way.", Notification.NotificationType.BROADCAST);
                emailService.sendPremiumActivatedEmail(email, fullName, planName, activatedAt);
                emailService.sendInvoiceEmail(email, new EmailService.InvoiceEmailDetails(
                        fullName,
                        planName,
                        amountPaise,
                        currency,
                        providerName,
                        providerOrderId,
                        providerPaymentId,
                        activatedAt));
            }
        } catch (Exception e) {
            log.error("Failed to process premium activated event", e);
        }
    }

    @KafkaListener(topics = "flowboard.account.status.changed", groupId = "notification-group")
    public void handleAccountStatusChanged(String rawPayload) {
        Map<String, Object> payload = parsePayload(rawPayload);
        log.info("Received account status event: {}", payload);
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            String email = (String) payload.get(EMAIL_FIELD);
            String fullName = payload.get(FULL_NAME_FIELD) != null ? payload.get(FULL_NAME_FIELD).toString() : "FlowBoard member";
            String status = payload.get("status").toString();

            if ("SUSPENDED".equalsIgnoreCase(status)) {
                saveNotification(userId, "Account suspended",
                        "Your FlowBoard account has been suspended. Contact support or your platform admin if you think this is a mistake.",
                        Notification.NotificationType.BROADCAST);
                emailService.sendAccountSuspendedEmail(email, fullName);
                return;
            }

            if ("RESTORED".equalsIgnoreCase(status)) {
                saveNotification(userId, "Account restored",
                        "Your FlowBoard account has been restored and you can sign in again.",
                        Notification.NotificationType.BROADCAST);
                emailService.sendAccountRestoredEmail(email, fullName);
            }
        } catch (Exception e) {
            log.error("Failed to process account status event", e);
        }
    }

    private Map<String, Object> parsePayload(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Kafka payload: " + rawPayload, e);
        }
    }

    private void saveNotification(Long recipientId, String title, String message, Notification.NotificationType type) {
        Notification n = new Notification();
        n.setRecipientId(recipientId);
        n.setTitle(title);
        n.setMessage(message);
        n.setIsRead(false);
        n.setType(type);
        notificationRepository.save(n);
    }

    private String resolveUserName(Long userId) {
        try {
            Map<String, Object> user = restTemplate.getForObject(buildAuthServiceUri(USERS_SEGMENT, userId.toString()), Map.class);
            if (user != null && user.get(FULL_NAME_FIELD) != null) {
                return (String) user.get(FULL_NAME_FIELD);
            }
        } catch (Exception e) {
            log.warn("Could not resolve user name for userId={}", userId);
        }
        return "A teammate";
    }

    private String resolveCardName(String cardId) {
        try {
            Map<String, Object> card = restTemplate.getForObject(buildCardServiceUri(cardId), Map.class);
            if (card != null && card.get("title") != null) {
                return "\"" + card.get("title") + "\"";
            }
        } catch (Exception e) {
            log.warn("Could not resolve card name for cardId={}", cardId);
        }
        return "Card #" + cardId;
    }

    private String buildAuthServiceUri(String... pathSegments) {
        return UriComponentsBuilder.fromHttpUrl(authServiceUrl)
                .pathSegment(pathSegments)
                .toUriString();
    }

    private String buildCardServiceUri(String cardId) {
        return UriComponentsBuilder.fromHttpUrl(cardServiceUrl)
                .pathSegment(cardId)
                .toUriString();
    }
}
