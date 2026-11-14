package com.smartstart.portal.centre.service;

import com.smartstart.portal.centre.entity.Centre;
import com.smartstart.portal.centre.repository.CentreRepository;
import com.smartstart.portal.centre.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** The centre the current request or job is working in. */
@Component
@RequiredArgsConstructor
public class CurrentCentre {

    private final CentreRepository repository;

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Centre get() {
        long id = TenantContext.require();
        return repository.findById(id).orElseThrow(() -> new IllegalStateException("Centre " + id + " not found"));
    }

    public String name() {
        return TenantContext.isSet() ? get().getName() : "";
    }
}
