package com.smartstart.portal.graderate.controller;

import com.smartstart.portal.graderate.dto.GradeRateDtos.CreateGradeRateRequest;
import com.smartstart.portal.graderate.dto.GradeRateDtos.GradeRateResponse;
import com.smartstart.portal.graderate.dto.GradeRateDtos.UpdateGradeRateRequest;
import com.smartstart.portal.graderate.service.GradeRateService;
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

@Tag(name = "Grade rates")
@RestController
@RequestMapping("/api/v1/grade-rates")
@RequiredArgsConstructor
public class GradeRateController {

    private final GradeRateService service;

    @GetMapping
    public List<GradeRateResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GradeRateResponse create(@Valid @RequestBody CreateGradeRateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public GradeRateResponse update(@PathVariable Long id, @Valid @RequestBody UpdateGradeRateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
