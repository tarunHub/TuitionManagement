package com.smartstart.portal.centre.repository;

import com.smartstart.portal.centre.entity.Centre;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CentreRepository extends JpaRepository<Centre, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    List<Centre> findAllByOrderByNameAsc();

    @Query("select c.id from Centre c where c.active = true order by c.id")
    List<Long> findActiveIds();

    /** Locks the row so two new students can't get the same code. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Centre c where c.id = :id")
    Optional<Centre> lockById(@Param("id") Long id);
}
