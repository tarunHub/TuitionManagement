package com.smartstart.portal.graderate.repository;

import com.smartstart.portal.graderate.entity.GradeRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GradeRateRepository extends JpaRepository<GradeRate, Long> {

    Optional<GradeRate> findByGradeIgnoreCase(String grade);

    boolean existsByGradeIgnoreCase(String grade);
}
