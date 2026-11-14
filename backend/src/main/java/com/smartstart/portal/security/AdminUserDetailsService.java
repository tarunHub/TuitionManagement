package com.smartstart.portal.security;

import com.smartstart.portal.auth.entity.AdminUser;
import com.smartstart.portal.auth.repository.AdminUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository repository;

    @Override
    @Transactional(readOnly = true)
    public PortalUser loadUserByUsername(String username) {
        AdminUser user = repository.findWithCentre(username)
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
        return toPrincipal(user);
    }

    public static PortalUser toPrincipal(AdminUser user) {
        return new PortalUser(user.getId(), user.getUsername(), user.getPasswordHash(), user.isEnabled(), user.getRole(),
                user.getCentre() == null ? null : user.getCentre().getId(),
                user.getCentre() != null && user.getCentre().isActive());
    }
}
