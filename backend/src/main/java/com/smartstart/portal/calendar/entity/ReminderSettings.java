package com.smartstart.portal.calendar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import org.hibernate.annotations.TenantId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** Automatic class reminder settings: one row per centre. */
@Getter
@Setter
@Entity
@Table(name = "reminder_settings")
public class ReminderSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @TenantId
    @Column(name = "centre_id", nullable = false, updatable = false)
    private Long centreId;

    @Column(nullable = false)
    private boolean enabled;

    /** 0 = on the morning of the class, 1 = the day before, 2 = two days before. */
    @Column(name = "days_before", nullable = false)
    private short daysBefore;

    @Column(name = "send_time", nullable = false)
    private LocalTime sendTime;

    @Column(name = "send_email", nullable = false)
    private boolean sendEmail;

    @Column(name = "send_sms", nullable = false)
    private boolean sendSms;

    @Column(name = "last_run_date")
    private LocalDate lastRunDate;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
