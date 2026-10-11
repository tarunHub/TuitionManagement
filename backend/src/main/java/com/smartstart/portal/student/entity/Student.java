package com.smartstart.portal.student.entity;

import com.smartstart.portal.classgroup.entity.ClassGroup;
import com.smartstart.portal.common.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "student")
public class Student extends BaseEntity {

    @Column(name = "student_code", nullable = false, unique = true, length = 10, updatable = false)
    private String studentCode;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 30)
    private String grade;

    @Column(length = 150)
    private String school;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "student_subject", joinColumns = @JoinColumn(name = "student_id"))
    @Column(name = "subject", length = 60)
    @OrderBy
    @BatchSize(size = 100)
    private Set<String> subjects = new LinkedHashSet<>();

    @Column(columnDefinition = "text")
    private String address;

    @Column(name = "guardian_name", nullable = false, length = 100)
    private String guardianName;

    @Column(name = "primary_contact", length = 20)
    private String primaryContact;

    @Column(name = "alternate_contact", length = 20)
    private String alternateContact;

    @Column(length = 150)
    private String email;

    /** Parent consented to SMS reminders at the primary contact number. */
    @Column(name = "sms_opt_in", nullable = false)
    private boolean smsOptIn;

    @Column(name = "emergency_contact_name", length = 100)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(name = "enrollment_date")
    private LocalDate enrollmentDate;

    @Column(name = "per_class_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal perClassRate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StudentStatus status = StudentStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_group_id")
    private ClassGroup classGroup;

    @Column(columnDefinition = "text")
    private String notes;

    public boolean isActive() {
        return status == StudentStatus.ACTIVE;
    }
}
