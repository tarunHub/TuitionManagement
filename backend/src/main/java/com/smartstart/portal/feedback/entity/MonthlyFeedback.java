package com.smartstart.portal.feedback.entity;

import com.smartstart.portal.common.entity.BaseEntity;
import com.smartstart.portal.student.entity.Student;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "monthly_feedback")
public class MonthlyFeedback extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "feedback_month", nullable = false)
    private LocalDate feedbackMonth;

    @Column(name = "topics_taught", nullable = false, columnDefinition = "text")
    private String topicsTaught;

    @Column(name = "improvement_areas", columnDefinition = "text")
    private String improvementAreas;

    @Column(columnDefinition = "text")
    private String achievements;
}
