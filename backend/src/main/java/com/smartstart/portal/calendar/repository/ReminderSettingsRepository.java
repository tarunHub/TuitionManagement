package com.smartstart.portal.calendar.repository;

import com.smartstart.portal.calendar.entity.ReminderSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderSettingsRepository extends JpaRepository<ReminderSettings, Short> {
}
