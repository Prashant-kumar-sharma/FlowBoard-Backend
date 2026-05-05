package com.flowboard.notification.service;

import com.flowboard.notification.entity.Notification;
import com.flowboard.notification.exception.ResourceNotFoundException;
import com.flowboard.notification.repository.NotificationRepository;
import com.flowboard.notification.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock NotificationRepository notificationRepository;
    @InjectMocks NotificationServiceImpl notificationService;

    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = Notification.builder()
                .id(1L).recipientId(10L).actorId(5L)
                .type(Notification.NotificationType.ASSIGNMENT)
                .title("You were assigned").message("Card #1 assigned to you")
                .isRead(false).build();
    }

    @Test
    void should_sendNotification_and_persist() {
        when(notificationRepository.save(any())).thenReturn(sampleNotification);

        Notification result = notificationService.send(new NotificationService.NotificationRequest(
                10L,
                5L,
                Notification.NotificationType.ASSIGNMENT,
                "Title",
                "Message",
                1L,
                "CARD",
                "/cards/1"));

        assertThat(result.getRecipientId()).isEqualTo(10L);
        assertThat(result.getType()).isEqualTo(Notification.NotificationType.ASSIGNMENT);
        verify(notificationRepository).save(any());
    }

    @Test
    void should_sendBulk_to_all_recipients() {
        when(notificationRepository.save(any())).thenReturn(sampleNotification);

        notificationService.sendBulk(
                List.of(1L, 2L, 3L),
                Notification.NotificationType.BROADCAST,
                "Broadcast", "Platform announcement");

        verify(notificationRepository, times(3)).save(any());
    }

    @Test
    void should_markAsRead_when_notificationExists() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(sampleNotification));
        when(notificationRepository.save(any())).thenReturn(sampleNotification);

        notificationService.markAsRead(1L, 10L);
        verify(notificationRepository).save(argThat(n -> n.getIsRead()));
    }

    @Test
    void should_throwResourceNotFoundException_when_notificationMissing() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> notificationService.markAsRead(99L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void should_markAllRead_for_recipient() {
        Notification unread1 = Notification.builder().id(1L).recipientId(10L).isRead(false).build();
        Notification unread2 = Notification.builder().id(2L).recipientId(10L).isRead(false).build();
        when(notificationRepository.findByRecipientIdAndIsRead(10L, false))
                .thenReturn(List.of(unread1, unread2));

        notificationService.markAllRead(10L);
        verify(notificationRepository, times(2)).save(any());
    }

    @Test
    void should_returnUnreadCount() {
        when(notificationRepository.countByRecipientIdAndIsRead(10L, false)).thenReturn(5L);
        assertThat(notificationService.getUnreadCount(10L)).isEqualTo(5L);
    }

    @Test
    void should_returnNotificationsForRecipient() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(10L))
                .thenReturn(List.of(sampleNotification));
        List<Notification> result = notificationService.getByRecipient(10L);
        assertThat(result).hasSize(1);
    }

    @Test
    void should_deleteReadNotificationsForRecipient() {
        notificationService.deleteRead(10L);

        verify(notificationRepository).deleteByRecipientIdAndIsReadTrue(10L);
    }

    @Test
    void should_deleteNotificationById() {
        notificationService.delete(1L, 10L);

        verify(notificationRepository).deleteById(1L);
    }
}
