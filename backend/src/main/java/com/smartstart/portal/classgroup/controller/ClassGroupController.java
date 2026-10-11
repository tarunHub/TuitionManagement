package com.smartstart.portal.classgroup.controller;

import com.smartstart.portal.classgroup.dto.ClassGroupDtos.ClassGroupResponse;
import com.smartstart.portal.classgroup.dto.ClassGroupDtos.CreateClassGroupRequest;
import com.smartstart.portal.classgroup.dto.ClassGroupDtos.UpdateClassGroupRequest;
import com.smartstart.portal.classgroup.service.ClassGroupService;
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

@Tag(name = "Class groups")
@RestController
@RequestMapping("/api/v1/class-groups")
@RequiredArgsConstructor
public class ClassGroupController {

    private final ClassGroupService service;

    @GetMapping
    public List<ClassGroupResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClassGroupResponse create(@Valid @RequestBody CreateClassGroupRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ClassGroupResponse update(@PathVariable Long id, @Valid @RequestBody UpdateClassGroupRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
