package com.smartstart.portal.security;

import com.smartstart.portal.centre.repository.CentreRepository;
import com.smartstart.portal.centre.tenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates the bearer token and picks the centre the request works in: a staff user's own centre, or for a
 * platform admin the centre named in the {@value #CENTRE_HEADER} header (when they have switched into one).
 * Centre-owned API calls without a centre are refused with 409 CENTRE_REQUIRED.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String CENTRE_HEADER = "X-Centre-Id";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AdminUserDetailsService userDetailsService;
    private final CentreRepository centres;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            PortalUser user = authenticate(request);
            if (user != null && needsCentre(request)) {
                Long centreId = user.isPlatformAdmin() ? requestedCentre(request) : user.centreId();
                if (centreId == null) {
                    response.setStatus(HttpServletResponse.SC_CONFLICT);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":409,\"error\":\"CENTRE_REQUIRED\",\"message\":\"Choose a centre first\"}");
                    return;
                }
                TenantContext.set(centreId);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private PortalUser authenticate(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(PREFIX) || SecurityContextHolder.getContext().getAuthentication() != null) {
            return null;
        }
        return jwtService.validate(header.substring(PREFIX.length()).trim()).map(username -> {
            try {
                PortalUser user = userDetailsService.loadUserByUsername(username);
                if (!user.isEnabled()) {
                    return null;
                }
                var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                return user;
            } catch (UsernameNotFoundException ignored) {
                return null; // token for a removed user: request stays unauthenticated
            }
        }).orElse(null);
    }

    /** Everything under /api except sign-in/profile and the platform admin's own screens works on one centre's data. */
    private static boolean needsCentre(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.startsWith("/api/") && !path.startsWith("/api/v1/auth/") && !path.startsWith("/api/v1/platform/");
    }

    private Long requestedCentre(HttpServletRequest request) {
        String raw = request.getHeader(CENTRE_HEADER);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            Long id = Long.valueOf(raw.trim());
            return centres.existsById(id) ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
