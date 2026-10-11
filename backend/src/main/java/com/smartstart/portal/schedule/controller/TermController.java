package com.smartstart.portal.schedule.controller;

import com.smartstart.portal.schedule.dto.ScheduleDtos.CreateTermRequest;
import com.smartstart.portal.schedule.dto.ScheduleDtos.TermResponse;
import com.smartstart.portal.schedule.dto.ScheduleDtos.UpdateTermRequest;
import com.smartstart.portal.schedule.service.TermService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Schedule")
@RestController
@RequestMapping("/api/v1/terms")
@RequiredArgsConstructor
public class TermController {

    private final TermService service;

    @GetMapping
    public List<TermResponse> list() {
        return service.list();
    }

    @Operation(summary = "The active term (204 when none is configured)")
    @GetMapping("/active")
    public ResponseEntity<TermResponse> active() {
        return service.active().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Create or update the active term")
    @PutMapping("/active")
    public TermResponse upsertActive(@Valid @RequestBody UpdateTermRequest request) {
        return service.upsertActive(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TermResponse create(@Valid @RequestBody CreateTermRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public TermResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTermRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Delete a term and its schedule slots")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
