package com.smartstart.portal.graderate.entity;

import com.smartstart.portal.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "grade_rate")
public class GradeRate extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String grade;

    @Column(name = "per_class_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal perClassRate;

    @Column(columnDefinition = "text")
    private String notes;
}
