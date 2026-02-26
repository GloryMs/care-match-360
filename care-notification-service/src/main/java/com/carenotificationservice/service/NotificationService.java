package com.carenotificationservice.service;

import com.carecommon.dto.ApiResponse;
import com.carecommon.exception.ResourceNotFoundException;
import com.carecommon.kafkaEvents.*;
import com.carenotificationservice.dto.NotificationResponse;
import com.carenotificationservice.dto.ProfileSummaryDTO;
import com.carenotificationservice.dto.SendNotificationRequest;
import com.carenotificationservice.feign.ProfileServiceClient;
import com.carenotificationservice.model.Notification;
import com.carenotificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final ProfileServiceClient profileServiceClient;

    @Transactional
    public NotificationResponse sendNotification(SendNotificationRequest request) {
        log.info("Sending notification: recipientId={}, type={}, channel={}",
                request.getRecipientId(), request.getType(), request.getChannel());

        Notification.NotificationType type = Notification.NotificationType.valueOf(request.getType().toUpperCase());

        Notification notification = Notification.builder()
                .recipientId(request.getRecipientId())
                .recipientEmail(request.getRecipientEmail())
                .type(type)
                .channel(request.getChannel())
                .subject(request.getSubject())
                .body(request.getBody())
                .status(Notification.NotificationStatus.PENDING)
                .build();

        notification = notificationRepository.save(notification);

        try {
            if (type == Notification.NotificationType.EMAIL) {
                sendEmailNotification(notification);
            } else if (type == Notification.NotificationType.IN_APP) {
                notification.setStatus(Notification.NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                notificationRepository.save(notification);
            }

            log.info("Notification sent successfully: notificationId={}", notification.getId());

        } catch (Exception e) {
            log.error("Failed to send notification: notificationId={}", notification.getId(), e);
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setErrorMessage(e.getMessage());
            notification.setRetryCount(notification.getRetryCount() + 1);
            notificationRepository.save(notification);
        }

        ModelMapper modelMapper = new ModelMapper();
        return modelMapper.map(notification, NotificationResponse.class);
    }

    @Transactional
    public void sendMatchNotification(MatchCalculatedEvent event) {
        try {
            String email = resolveEmailByProviderProfileId(event.getProviderId());

            String subject = "New High-Quality Match Found!";
            String body = String.format(
                    "Great news! A new patient with a match score of %.0f%% has been identified. " +
                            "Log in to view details and send an offer.",
                    event.getScore().doubleValue()
            );

            SendNotificationRequest request = SendNotificationRequest.builder()
                    .recipientId(event.getProviderId())
                    .recipientEmail(email)
                    .type("EMAIL")
                    .channel("new_match")
                    .subject(subject)
                    .body(body)
                    .build();

            sendNotification(request);

        } catch (Exception e) {
            log.error("Failed to send match notification: matchId={}", event.getMatchId(), e);
        }
    }

    @Transactional
    public void sendOfferReceivedNotification(OfferSentEvent event) {
        try {
            String email = resolveEmailByPatientProfileId(event.getPatientId());

            String subject = "You Have Received a New Care Offer!";
            String body = "A care provider has sent you an offer. Log in to review the details and respond.";

            SendNotificationRequest request = SendNotificationRequest.builder()
                    .recipientId(event.getPatientId())
                    .recipientEmail(email)
                    .type("EMAIL")
                    .channel("offer_received")
                    .subject(subject)
                    .body(body)
                    .build();

            sendNotification(request);

        } catch (Exception e) {
            log.error("Failed to send offer received notification: offerId={}", event.getOfferId(), e);
        }
    }

    @Transactional
    public void sendOfferAcceptedNotification(OfferAcceptedEvent event) {
        try {
            String email = resolveEmailByProviderProfileId(event.getProviderId());

            String subject = "Your Offer Has Been Accepted!";
            String body = "Congratulations! A patient has accepted your care offer. Please log in to proceed with the next steps.";

            SendNotificationRequest request = SendNotificationRequest.builder()
                    .recipientId(event.getProviderId())
                    .recipientEmail(email)
                    .type("EMAIL")
                    .channel("offer_accepted")
                    .subject(subject)
                    .body(body)
                    .build();

            sendNotification(request);

        } catch (Exception e) {
            log.error("Failed to send offer accepted notification: offerId={}", event.getOfferId(), e);
        }
    }

    @Transactional
    public void sendPaymentSuccessNotification(PaymentSucceededEvent event) {
        try {
            String email = resolveEmailByProviderProfileId(event.getProviderId());

            String subject = "Payment Confirmation";
            String body = String.format(
                    "Your payment of %s %s has been processed successfully. Thank you for your subscription!",
                    event.getAmount(), event.getCurrency()
            );

            SendNotificationRequest request = SendNotificationRequest.builder()
                    .recipientId(event.getProviderId())
                    .recipientEmail(email)
                    .type("EMAIL")
                    .channel("payment_success")
                    .subject(subject)
                    .body(body)
                    .build();

            sendNotification(request);

        } catch (Exception e) {
            log.error("Failed to send payment success notification: invoiceId={}", event.getInvoiceId(), e);
        }
    }

    @Transactional
    public void sendSubscriptionExpiredNotification(SubscriptionExpiredEvent event) {
        try {
            String email = resolveEmailByProviderProfileId(event.getProviderId());

            String subject = "Subscription Payment Overdue";
            String body = "Your subscription payment is overdue. Please update your payment method to continue accessing all features.";

            SendNotificationRequest request = SendNotificationRequest.builder()
                    .recipientId(event.getProviderId())
                    .recipientEmail(email)
                    .type("EMAIL")
                    .channel("subscription_expired")
                    .subject(subject)
                    .body(body)
                    .build();

            sendNotification(request);

        } catch (Exception e) {
            log.error("Failed to send subscription expired notification: subscriptionId={}",
                    event.getSubscriptionId(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForRecipient(UUID recipientId, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20);

        Page<Notification> notificationsPage = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(
                recipientId, pageable);

        ModelMapper modelMapper = new ModelMapper();
        return notificationsPage.getContent().stream()
                .map(notification -> modelMapper.map(notification, NotificationResponse.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(UUID recipientId) {
        List<Notification> notifications = notificationRepository.findUnreadNotificationsByRecipientId(recipientId);

        ModelMapper modelMapper = new ModelMapper();
        return notifications.stream()
                .map(notification -> modelMapper.map(notification, NotificationResponse.class))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID recipientId) {
        return notificationRepository.countByRecipientIdAndReadAtIsNull(recipientId);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
            log.info("Notification marked as read: notificationId={}", notificationId);
        }
    }

    @Transactional
    public void markAllAsRead(UUID recipientId) {
        List<Notification> unreadNotifications = notificationRepository
                .findUnreadNotificationsByRecipientId(recipientId);

        LocalDateTime now = LocalDateTime.now();
        unreadNotifications.forEach(notification -> notification.setReadAt(now));

        notificationRepository.saveAll(unreadNotifications);
        log.info("All notifications marked as read: recipientId={}, count={}", recipientId, unreadNotifications.size());
    }

    @Transactional
    @Scheduled(cron = "0 */15 * * * ?") // Every 15 minutes
    public void retryFailedNotifications() {
        log.info("Retrying failed notifications");

        List<Notification> failedNotifications = notificationRepository.findFailedNotificationsForRetry();

        for (Notification notification : failedNotifications) {
            try {
                if (notification.getType() == Notification.NotificationType.EMAIL) {
                    sendEmailNotification(notification);
                }

                log.info("Notification retry successful: notificationId={}", notification.getId());

            } catch (Exception e) {
                log.error("Notification retry failed: notificationId={}", notification.getId(), e);
                notification.setRetryCount(notification.getRetryCount() + 1);
                notification.setErrorMessage(e.getMessage());
                notificationRepository.save(notification);
            }
        }

        log.info("Processed {} failed notifications", failedNotifications.size());
    }

    // ─── Private helpers ────────────────────────────────────────────────────────

    private void sendEmailNotification(Notification notification) {
        String email = notification.getRecipientEmail();
        if (email == null || email.isBlank()) {
            log.warn("Skipping email — recipientEmail not set for notificationId={}", notification.getId());
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setErrorMessage("recipientEmail is required for EMAIL notifications");
            notificationRepository.save(notification);
            return;
        }

        emailService.sendSimpleEmail(email, notification.getSubject(), notification.getBody());

        notification.setStatus(Notification.NotificationStatus.SENT);
        notification.setSentAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    private String resolveEmailByPatientProfileId(UUID patientProfileId) {
        try {
            ApiResponse<ProfileSummaryDTO> profileResponse =
                    profileServiceClient.getPatientProfile(patientProfileId);
            String email = profileResponse.getData().getEmail();
            if (email == null || email.isBlank()) {
                throw new RuntimeException("Email not available for patient profile " + patientProfileId);
            }
            return email;
        } catch (Exception e) {
            log.error("Failed to resolve email for patientProfileId={}", patientProfileId, e);
            throw new RuntimeException("Could not resolve recipient email for patient profile " + patientProfileId, e);
        }
    }

    private String resolveEmailByProviderProfileId(UUID providerProfileId) {
        try {
            ApiResponse<ProfileSummaryDTO> profileResponse =
                    profileServiceClient.getProviderProfile(providerProfileId);
            String email = profileResponse.getData().getEmail();
            if (email == null || email.isBlank()) {
                throw new RuntimeException("Email not available for provider profile " + providerProfileId);
            }
            return email;
        } catch (Exception e) {
            log.error("Failed to resolve email for providerProfileId={}", providerProfileId, e);
            throw new RuntimeException("Could not resolve recipient email for provider profile " + providerProfileId, e);
        }
    }
}
