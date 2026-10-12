package com.smartstart.portal.calendar.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

/** The single row of automatic class reminder settings (id is always 1). */
@Getter
@Setter
@Entity
@Table(name = "reminder_settings")
public class ReminderSettings {

    public static final short ID = 1;

    @Id
    private Short id;

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
