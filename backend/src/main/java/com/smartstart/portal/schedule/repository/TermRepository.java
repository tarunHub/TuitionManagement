package com.smartstart.portal.schedule.repository;

import com.smartstart.portal.schedule.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Long> {

    Optional<Term> findFirstByActiveTrue();

    List<Term> findAllByOrderByStartDateDesc();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Term t set t.active = false where t.active = true and t.id <> :keepId")
    int deactivateOthers(@Param("keepId") Long keepId);
}
