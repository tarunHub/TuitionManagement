package com.smartstart.portal.security;

import com.smartstart.portal.auth.entity.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** The logged-in user: platform admin (no centre) or staff of one centre. */
public record PortalUser(Long id, String username, String passwordHash, boolean enabled, UserRole role,
                         Long centreId, boolean centreActive) implements UserDetails {

    public boolean isPlatformAdmin() {
        return role == UserRole.PLATFORM_ADMIN;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    /** Staff of a deactivated centre can't sign in. */
    @Override
    public boolean isEnabled() {
        return enabled && (isPlatformAdmin() || centreActive);
    }
}
