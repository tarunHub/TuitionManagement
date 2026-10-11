package com.smartstart.portal.attendance.repository;

import com.smartstart.portal.attendance.dto.AttendanceDtos.AttendanceCell;
import com.smartstart.portal.attendance.dto.AttendanceDtos.StudentAttendanceCount;
import com.smartstart.portal.attendance.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    boolean existsByStudentId(Long studentId);

    @Query("""
            select new com.smartstart.portal.attendance.dto.AttendanceDtos$AttendanceCell(a.student.id, a.classDate, a.status)
            from Attendance a
            where a.classDate between :from and :to and a.student.id in :studentIds
            """)
    List<AttendanceCell> findCells(@Param("studentIds") Collection<Long> studentIds,
                                   @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select a from Attendance a
            where a.student.id in :studentIds and a.classDate between :from and :to
            """)
    List<Attendance> findForUpdate(@Param("studentIds") Collection<Long> studentIds,
                                   @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Per-student P/A counts for a date range (all students that have rows). */
    @Query("""
            select new com.smartstart.portal.attendance.dto.AttendanceDtos$StudentAttendanceCount(
                a.student.id,
                sum(case when a.status = com.smartstart.portal.attendance.entity.AttendanceStatus.PRESENT then 1L else 0L end),
                sum(case when a.status = com.smartstart.portal.attendance.entity.AttendanceStatus.ABSENT then 1L else 0L end))
            from Attendance a
            where a.classDate between :from and :to
            group by a.student.id
            """)
    List<StudentAttendanceCount> countByStudent(@Param("from") LocalDate from, @Param("to") LocalDate to);

    List<Attendance> findByStudentIdOrderByClassDateDesc(Long studentId);
}
