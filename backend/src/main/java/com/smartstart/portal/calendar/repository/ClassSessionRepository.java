package com.smartstart.portal.calendar.repository;

import com.smartstart.portal.calendar.entity.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {

    /** Sessions between two dates (inclusive), optionally for one batch, in date then batch order. */
    @Query("""
            select s from ClassSession s join fetch s.classGroup g
            where s.classDate between :from and :to and (:groupId is null or g.id = :groupId)
            order by s.classDate, s.startTime, g.name""")
    List<ClassSession> findBetween(@Param("from") LocalDate from, @Param("to") LocalDate to, @Param("groupId") Long groupId);

    boolean existsByClassGroupIdAndClassDate(Long classGroupId, LocalDate classDate);

    boolean existsByClassGroupIdAndClassDateAndIdNot(Long classGroupId, LocalDate classDate, Long id);
}
