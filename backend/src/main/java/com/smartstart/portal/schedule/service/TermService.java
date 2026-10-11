package com.smartstart.portal.schedule.service;

import com.smartstart.portal.common.exception.BadRequestException;
import com.smartstart.portal.common.exception.NotFoundException;
import com.smartstart.portal.schedule.dto.ScheduleDtos.CreateTermRequest;
import com.smartstart.portal.schedule.dto.ScheduleDtos.TermFields;
import com.smartstart.portal.schedule.dto.ScheduleDtos.TermResponse;
import com.smartstart.portal.schedule.dto.ScheduleDtos.UpdateTermRequest;
import com.smartstart.portal.schedule.entity.Term;
import com.smartstart.portal.schedule.mapper.ScheduleMapper;
import com.smartstart.portal.schedule.repository.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TermService {

    private final TermRepository repository;
    private final ScheduleMapper mapper;

    @Transactional(readOnly = true)
    public List<TermResponse> list() {
        return repository.findAllByOrderByStartDateDesc().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Optional<TermResponse> active() {
        return repository.findFirstByActiveTrue().map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<Term> activeTerm() {
        return repository.findFirstByActiveTrue();
    }

    /** Creates or updates the active term (the "term config" card). */
    @Transactional
    public TermResponse upsertActive(UpdateTermRequest request) {
        Term term = repository.findFirstByActiveTrue().orElseGet(Term::new);
        apply(term, request);
        term.setActive(true);
        Term saved = repository.saveAndFlush(term);
        repository.deactivateOthers(saved.getId());
        return mapper.toResponse(saved);
    }

    @Transactional
    public TermResponse create(CreateTermRequest request) {
        Term term = new Term();
        apply(term, request);
        boolean active = Boolean.TRUE.equals(request.active());
        if (active) {
            repository.findFirstByActiveTrue().ifPresent(t -> {
                t.setActive(false);
                repository.saveAndFlush(t);
            });
        }
        term.setActive(active);
        return mapper.toResponse(repository.saveAndFlush(term));
    }

    @Transactional
    public TermResponse update(Long id, UpdateTermRequest request) {
        require(id);
        if (Boolean.TRUE.equals(request.active())) {
            // Only one active term at a time: switch the others off first (clears the persistence context).
            repository.deactivateOthers(id);
        }
        Term term = require(id);
        apply(term, request);
        if (request.active() != null) {
            term.setActive(request.active());
        }
        return mapper.toResponse(repository.saveAndFlush(term));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(require(id));
    }

    public Term require(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Term", id));
    }

    private static void apply(Term term, TermFields request) {
        if (!request.endDate().isAfter(request.startDate())) {
            throw new BadRequestException("VALIDATION_FAILED", "must be after the start date", "endDate");
        }
        term.setName(request.name().trim());
        term.setStartDate(request.startDate());
        term.setEndDate(request.endDate());
        term.setCalendarName(request.calendarName() == null || request.calendarName().isBlank() ? null : request.calendarName().trim());
    }
}
