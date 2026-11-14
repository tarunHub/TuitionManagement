package com.smartstart.portal.centre.service;

import com.smartstart.portal.centre.repository.CentreRepository;
import com.smartstart.portal.centre.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/** Runs background work once per active centre, each inside that centre's data. One centre failing doesn't stop the rest. */
@Slf4j
@Component
@RequiredArgsConstructor
public class EachCentre {

    private final CentreRepository centres;

    public void run(String what, Consumer<Long> work) {
        for (Long id : centres.findActiveIds()) {
            try {
                TenantContext.runAs(id, () -> work.accept(id));
            } catch (RuntimeException e) {
                log.error("{} failed for centre {}", what, id, e);
            }
        }
    }
}
