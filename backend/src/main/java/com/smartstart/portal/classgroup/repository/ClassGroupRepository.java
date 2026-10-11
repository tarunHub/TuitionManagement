package com.smartstart.portal.classgroup.repository;

import com.smartstart.portal.classgroup.entity.ClassGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassGroupRepository extends JpaRepository<ClassGroup, Long> {

    boolean existsByNameIgnoreCase(String name);
}
