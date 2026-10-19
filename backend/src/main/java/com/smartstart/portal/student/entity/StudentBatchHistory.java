package com.smartstart.portal.student.entity;

import com.smartstart.portal.classgroup.entity.ClassGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

/** The batch a student is in from {@code fromMonth} until their next history row (null = no batch). */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "student_batch_history")
public class StudentBatchHistory {

    @EmbeddedId
    private Key id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_group_id")
    private ClassGroup classGroup;

    public StudentBatchHistory(Long studentId, LocalDate fromMonth, ClassGroup classGroup) {
        this.id = new Key(studentId, fromMonth);
        this.classGroup = classGroup;
    }

    @Embeddable
    public record Key(@Column(name = "student_id") Long studentId,
                      @Column(name = "from_month") LocalDate fromMonth) implements Serializable {
    }
}
