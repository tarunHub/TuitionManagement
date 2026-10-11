package com.smartstart.portal.student.controller;

import com.smartstart.portal.common.dto.PageResponse;
import com.smartstart.portal.common.util.FileResponses;
import com.smartstart.portal.student.dto.StudentDtos.CreateStudentRequest;
import com.smartstart.portal.student.dto.StudentDtos.StudentFilter;
import com.smartstart.portal.student.dto.StudentDtos.StudentResponse;
import com.smartstart.portal.student.dto.StudentDtos.UpdateStudentRequest;
import com.smartstart.portal.student.dto.StudentDtos.UpdateStudentStatusRequest;
import com.smartstart.portal.student.entity.StudentStatus;
import com.smartstart.portal.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Students")
@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService service;

    @Operation(summary = "Paginated, searchable student list")
    @GetMapping
    public PageResponse<StudentResponse> list(@RequestParam(required = false) String search,
                                              @RequestParam(required = false) StudentStatus status,
                                              @RequestParam(required = false) String grade,
                                              @RequestParam(required = false) Long groupId,
                                              @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return service.list(new StudentFilter(search, status, grade, groupId), pageable);
    }

    @Operation(summary = "Export students (respecting filters) as CSV")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String search,
                                         @RequestParam(required = false) StudentStatus status,
                                         @RequestParam(required = false) String grade,
                                         @RequestParam(required = false) Long groupId) {
        return FileResponses.csv(service.exportCsv(new StudentFilter(search, status, grade, groupId)), "students.csv");
    }

    @GetMapping("/{id}")
    public StudentResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @Operation(summary = "Create a student; the student code is generated")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StudentResponse create(@Valid @RequestBody CreateStudentRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody UpdateStudentRequest request) {
        return service.update(id, request);
    }

    @Operation(summary = "Activate or deactivate a student")
    @PatchMapping("/{id}/status")
    public StudentResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStudentStatusRequest request) {
        return service.updateStatus(id, request.status());
    }

    @Operation(summary = "Hard delete when there is no history; otherwise 409 (deactivate instead)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
