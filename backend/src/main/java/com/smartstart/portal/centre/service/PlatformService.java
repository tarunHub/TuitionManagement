package com.smartstart.portal.centre.service;

import com.smartstart.portal.auth.entity.AdminUser;
import com.smartstart.portal.auth.entity.UserRole;
import com.smartstart.portal.auth.repository.AdminUserRepository;
import com.smartstart.portal.centre.dto.CentreDtos.CentreRequest;
import com.smartstart.portal.centre.dto.CentreDtos.CentreResponse;
import com.smartstart.portal.centre.dto.CentreDtos.CreateCentreRequest;
import com.smartstart.portal.centre.dto.CentreDtos.ResetPasswordRequest;
import com.smartstart.portal.centre.dto.CentreDtos.UpdateUserRequest;
import com.smartstart.portal.centre.dto.CentreDtos.UserRequest;
import com.smartstart.portal.centre.dto.CentreDtos.UserResponse;
import com.smartstart.portal.centre.entity.Centre;
import com.smartstart.portal.centre.repository.CentreRepository;
import com.smartstart.portal.centre.tenancy.TenantContext;
import com.smartstart.portal.common.exception.BadRequestException;
import com.smartstart.portal.common.exception.ConflictException;
import com.smartstart.portal.common.exception.NotFoundException;
import com.smartstart.portal.security.CurrentUser;
import com.smartstart.portal.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** The platform admin's work: onboarding centres and managing every login. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformService {

    private final CentreRepository centres;
    private final AdminUserRepository users;
    private final StudentRepository students;
    private final PasswordEncoder passwordEncoder;

    // ------------------------------------------------------------------ Centres

    /** Not transactional on purpose: each centre's student count runs in its own session, inside that centre. */
    public List<CentreResponse> listCentres() {
        return centres.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    public CentreResponse getCentre(Long id) {
        return toResponse(requireCentre(id));
    }

    @Transactional
    public CentreResponse createCentre(CreateCentreRequest request) {
        if (request.centre() == null) {
            throw new BadRequestException("VALIDATION_FAILED", "is required", "centre");
        }
        CentreRequest r = request.centre();
        String name = r.name().strip();
        if (centres.existsByNameIgnoreCase(name)) {
            throw new ConflictException("CENTRE_EXISTS", "A centre named '" + name + "' already exists");
        }
        if (request.firstUser() != null) {
            checkUsernameFree(request.firstUser().username());
        }
        Centre c = new Centre();
        apply(c, r);
        c.setActive(r.active() == null || r.active());
        c.setNextStudentCode(1);
        Centre saved = centres.saveAndFlush(c);
        if (request.firstUser() != null) {
            createStaff(saved, request.firstUser());
        }
        log.info("Created centre {} ({})", saved.getId(), saved.getName());
        return toResponseSimple(saved, request.firstUser() == null ? 0 : 1, 0);
    }

    @Transactional
    public CentreResponse updateCentre(Long id, CentreRequest r) {
        Centre c = requireCentre(id);
        String name = r.name().strip();
        if (centres.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("CENTRE_EXISTS", "A centre named '" + name + "' already exists");
        }
        apply(c, r);
        if (r.active() != null) {
            c.setActive(r.active());
        }
        return toResponseSimple(centres.saveAndFlush(c), users.countByCentreId(id), -1);
    }

    // ------------------------------------------------------------------ Users

    @Transactional(readOnly = true)
    public List<UserResponse> centreUsers(Long centreId) {
        requireCentre(centreId);
        return users.findByCentreIdOrderByUsernameAsc(centreId).stream().map(PlatformService::toUser).toList();
    }

    @Transactional
    public UserResponse addCentreUser(Long centreId, UserRequest request) {
        Centre centre = requireCentre(centreId);
        checkUsernameFree(request.username());
        return toUser(createStaff(centre, request));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> platformAdmins() {
        return users.findByRoleOrderByUsernameAsc(UserRole.PLATFORM_ADMIN).stream().map(PlatformService::toUser).toList();
    }

    @Transactional
    public UserResponse addPlatformAdmin(UserRequest request) {
        checkUsernameFree(request.username());
        AdminUser u = newUser(request);
        u.setRole(UserRole.PLATFORM_ADMIN);
        return toUser(users.save(u));
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        AdminUser u = requireUser(id);
        if (request.fullName() != null) {
            u.setFullName(request.fullName().isBlank() ? null : request.fullName().strip());
        }
        if (request.enabled() != null && request.enabled() != u.isEnabled()) {
            if (!request.enabled()) {
                guardLastAdmin(u, "disable");
                if (u.getUsername().equalsIgnoreCase(CurrentUser.username())) {
                    throw new BadRequestException("CANNOT_DISABLE_SELF", "You can't disable your own login", "enabled");
                }
            }
            u.setEnabled(request.enabled());
        }
        return toUser(u);
    }

    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        AdminUser u = requireUser(id);
        u.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        log.info("Password reset for user {}", u.getId());
    }

    @Transactional
    public void deleteUser(Long id) {
        AdminUser u = requireUser(id);
        if (u.getUsername().equalsIgnoreCase(CurrentUser.username())) {
            throw new BadRequestException("CANNOT_DELETE_SELF", "You can't delete your own login");
        }
        guardLastAdmin(u, "delete");
        users.delete(u);
    }

    // ------------------------------------------------------------------ Helpers

    private AdminUser createStaff(Centre centre, UserRequest request) {
        AdminUser u = newUser(request);
        u.setRole(UserRole.STAFF);
        u.setCentre(centre);
        return users.save(u);
    }

    private AdminUser newUser(UserRequest request) {
        AdminUser u = new AdminUser();
        u.setUsername(request.username().strip());
        u.setFullName(request.fullName() == null || request.fullName().isBlank() ? null : request.fullName().strip());
        u.setPasswordHash(passwordEncoder.encode(request.password()));
        u.setEnabled(true);
        return u;
    }

    private void checkUsernameFree(String username) {
        if (users.existsByUsernameIgnoreCase(username.strip())) {
            throw new ConflictException("USERNAME_TAKEN", "The username '" + username.strip() + "' is already taken");
        }
    }

    private void guardLastAdmin(AdminUser u, String action) {
        if (u.isPlatformAdmin() && u.isEnabled()
                && users.findByRoleOrderByUsernameAsc(UserRole.PLATFORM_ADMIN).stream().filter(AdminUser::isEnabled).count() <= 1) {
            throw new BadRequestException("LAST_ADMIN", "You can't " + action + " the last platform admin");
        }
    }

    private static void apply(Centre c, CentreRequest r) {
        c.setName(r.name().strip());
        c.setStudentCodePrefix(r.studentCodePrefix().strip().toUpperCase(Locale.ROOT));
        c.setContactEmail(blankToNull(r.contactEmail()));
        c.setContactPhone(blankToNull(r.contactPhone()));
        c.setAddress(blankToNull(r.address()));
    }

    private Centre requireCentre(Long id) {
        return centres.findById(id).orElseThrow(() -> new NotFoundException("Centre", id));
    }

    private AdminUser requireUser(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User", id));
    }

    private CentreResponse toResponse(Centre c) {
        long studentCount = TenantContext.callAs(c.getId(), students::count);
        return toResponseSimple(c, users.countByCentreId(c.getId()), studentCount);
    }

    private static CentreResponse toResponseSimple(Centre c, long userCount, long studentCount) {
        return new CentreResponse(c.getId(), c.getName(), c.getStudentCodePrefix(), c.getContactEmail(), c.getContactPhone(),
                c.getAddress(), c.isActive(), userCount, studentCount, c.emailConfigured(), c.smsConfigured(), c.getCreatedAt());
    }

    private static UserResponse toUser(AdminUser u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getFullName(), u.getRole().name(), u.isEnabled(),
                u.getCentre() == null ? null : u.getCentre().getId(), u.getCreatedAt());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }
}
