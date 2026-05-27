package ro.upt.pontaje.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.upt.pontaje.exception.ResourceNotFoundException;
import ro.upt.pontaje.model.Notification;
import ro.upt.pontaje.model.NotificationType;
import ro.upt.pontaje.model.User;
import ro.upt.pontaje.repository.NotificationRepository;

import java.util.List;
import java.util.UUID;

/**
 * Serviciu pentru gestionarea notificărilor in-app
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Returnează toate notificările unui utilizator (cele mai recente primele)
     */
    @Transactional(readOnly = true)
    public List<Notification> getForUser(User user) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    /**
     * Returnează numărul de notificări necitite
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(User user) {
        return notificationRepository.countByUserIdAndIsReadFalse(user.getId());
    }

    /**
     * Marchează o notificare ca citită
     */
    @Transactional
    public Notification markAsRead(UUID notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificarea nu a fost găsită"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new ro.upt.pontaje.exception.BadRequestException("Nu aveți acces la această notificare");
        }

        notification.markAsRead();
        return notificationRepository.save(notification);
    }

    /**
     * Marchează toate notificările ca citite
     */
    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllReadByUserId(user.getId());
    }

    /**
     * Creează o notificare pentru un utilizator
     */
    @Transactional
    public Notification create(User user, NotificationType type, String subject, String message) {
        Notification notification = Notification.builder()
                .user(user)
                .notificationType(type)
                .subject(subject)
                .message(message)
                .isRead(false)
                .isSent(false)
                .build();
        return notificationRepository.save(notification);
    }
}
