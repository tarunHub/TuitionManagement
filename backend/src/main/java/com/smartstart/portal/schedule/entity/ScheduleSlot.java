package com.smartstart.portal.schedule.entity;

import com.smartstart.portal.classgroup.entity.ClassGroup;
import com.smartstart.portal.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "schedule_slot")
public class ScheduleSlot extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_group_id", nullable = false)
    private ClassGroup classGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 3)
    private ScheduleDay dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(length = 150)
    private String subject;

    @Column(length = 150)
    private String location;

    @Column(length = 100)
    private String teacher;

    /** Normalised comma-separated list of additional recipients. */
    @Column(name = "extra_emails", columnDefinition = "text")
    private String extraEmails;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "google_event_id")
    private String googleEventId;
}
