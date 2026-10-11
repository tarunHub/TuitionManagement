package com.smartstart.portal.notification.repository;

import com.smartstart.portal.notification.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
