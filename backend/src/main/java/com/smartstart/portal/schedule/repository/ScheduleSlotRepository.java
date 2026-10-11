package com.smartstart.portal.schedule.repository;

import com.smartstart.portal.schedule.entity.ScheduleSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, Long> {

    long countByClassGroupId(Long classGroupId);

    @Query("""
            select s from ScheduleSlot s join fetch s.term t join fetch s.classGroup g
            where t.id = :termId
            order by s.dayOfWeek, s.startTime, g.name
            """)
    List<ScheduleSlot> findByTerm(@Param("termId") Long termId);

    /** Slots whose term overlaps [from, to]. */
    @Query("""
            select s from ScheduleSlot s join fetch s.term t join fetch s.classGroup g
            where t.startDate <= :to and t.endDate >= :from
            """)
    List<ScheduleSlot> findInTermsOverlapping(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select s from ScheduleSlot s join fetch s.term join fetch s.classGroup where s.id = :id")
    Optional<ScheduleSlot> findWithRelations(@Param("id") Long id);
}
