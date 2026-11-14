package com.smartstart.portal.auth.repository;

import com.smartstart.portal.auth.entity.AdminUser;
import com.smartstart.portal.auth.entity.UserRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    Optional<AdminUser> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = "centre")
    @Query("select u from AdminUser u where lower(u.username) = lower(:username)")
    Optional<AdminUser> findWithCentre(@Param("username") String username);

    List<AdminUser> findByCentreIdOrderByUsernameAsc(Long centreId);

    List<AdminUser> findByRoleOrderByUsernameAsc(UserRole role);

    long countByCentreId(Long centreId);
}
