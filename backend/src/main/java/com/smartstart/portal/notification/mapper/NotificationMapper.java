package com.smartstart.portal.notification.mapper;

import com.smartstart.portal.notification.dto.NotificationDtos.CreateTemplateRequest;
import com.smartstart.portal.notification.dto.NotificationDtos.NotificationResponse;
import com.smartstart.portal.notification.dto.NotificationDtos.TemplateResponse;
import com.smartstart.portal.notification.dto.NotificationDtos.UpdateTemplateRequest;
import com.smartstart.portal.notification.entity.Notification;
import com.smartstart.portal.notification.entity.NotificationTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
public interface NotificationMapper {

    @Mapping(target = "recipients", expression = "java(n.recipientList())")
    @Mapping(target = "recipientCount", expression = "java(n.recipientList().size())")
    @Mapping(target = "templateId", source = "template.id")
    NotificationResponse toResponse(Notification n);

    TemplateResponse toResponse(NotificationTemplate template);

    NotificationTemplate toEntity(CreateTemplateRequest request);

    void update(UpdateTemplateRequest request, @MappingTarget NotificationTemplate template);
}
