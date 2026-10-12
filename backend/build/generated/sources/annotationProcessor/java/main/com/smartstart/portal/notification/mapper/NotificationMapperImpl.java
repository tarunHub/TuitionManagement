package com.smartstart.portal.notification.mapper;

import com.smartstart.portal.notification.dto.NotificationDtos;
import com.smartstart.portal.notification.entity.Notification;
import com.smartstart.portal.notification.entity.NotificationChannel;
import com.smartstart.portal.notification.entity.NotificationStatus;
import com.smartstart.portal.notification.entity.NotificationTemplate;
import com.smartstart.portal.notification.entity.NotificationType;
import java.time.Instant;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-10T22:43:57-0400",
    comments = "version: 1.6.3, compiler: IncrementalProcessingEnvironment from gradle-language-java-9.0.0.jar, environment: Java 25.0.2 (Oracle Corporation)"
)
@Component
public class NotificationMapperImpl implements NotificationMapper {

    @Override
    public NotificationDtos.NotificationResponse toResponse(Notification n) {
        if ( n == null ) {
            return null;
        }

        Long templateId = null;
        Long id = null;
        NotificationType type = null;
        NotificationChannel channel = null;
        String subject = null;
        String body = null;
        NotificationStatus status = null;
        Instant scheduledAt = null;
        Instant sentAt = null;
        String errorMessage = null;
        Instant createdAt = null;
        Instant updatedAt = null;

        templateId = nTemplateId( n );
        id = n.getId();
        type = n.getType();
        channel = n.getChannel();
        subject = n.getSubject();
        body = n.getBody();
        status = n.getStatus();
        scheduledAt = n.getScheduledAt();
        sentAt = n.getSentAt();
        errorMessage = n.getErrorMessage();
        createdAt = n.getCreatedAt();
        updatedAt = n.getUpdatedAt();

        List<String> recipients = n.recipientList();
        int recipientCount = n.recipientList().size();

        NotificationDtos.NotificationResponse notificationResponse = new NotificationDtos.NotificationResponse( id, type, channel, subject, body, recipients, recipientCount, status, scheduledAt, sentAt, errorMessage, templateId, createdAt, updatedAt );

        return notificationResponse;
    }

    @Override
    public NotificationDtos.TemplateResponse toResponse(NotificationTemplate template) {
        if ( template == null ) {
            return null;
        }

        Long id = null;
        String name = null;
        NotificationType type = null;
        String subject = null;
        String body = null;
        Instant createdAt = null;
        Instant updatedAt = null;

        id = template.getId();
        name = template.getName();
        type = template.getType();
        subject = template.getSubject();
        body = template.getBody();
        createdAt = template.getCreatedAt();
        updatedAt = template.getUpdatedAt();

        NotificationDtos.TemplateResponse templateResponse = new NotificationDtos.TemplateResponse( id, name, type, subject, body, createdAt, updatedAt );

        return templateResponse;
    }

    @Override
    public NotificationTemplate toEntity(NotificationDtos.CreateTemplateRequest request) {
        if ( request == null ) {
            return null;
        }

        NotificationTemplate notificationTemplate = new NotificationTemplate();

        notificationTemplate.setName( request.name() );
        notificationTemplate.setType( request.type() );
        notificationTemplate.setSubject( request.subject() );
        notificationTemplate.setBody( request.body() );

        return notificationTemplate;
    }

    @Override
    public void update(NotificationDtos.UpdateTemplateRequest request, NotificationTemplate template) {
        if ( request == null ) {
            return;
        }

        template.setName( request.name() );
        template.setType( request.type() );
        template.setSubject( request.subject() );
        template.setBody( request.body() );
    }

    private Long nTemplateId(Notification notification) {
        NotificationTemplate template = notification.getTemplate();
        if ( template == null ) {
            return null;
        }
        return template.getId();
    }
}
