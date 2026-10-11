package com.smartstart.portal.notification.repository;

import com.smartstart.portal.notification.entity.Notification;
import com.smartstart.portal.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    @Query("select n.id from Notification n where n.status = :status and n.scheduledAt <= :now order by n.scheduledAt")
    List<Long> findDueIds(@Param("status") NotificationStatus status, @Param("now") Instant now);
}
