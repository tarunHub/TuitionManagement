package com.smartstart.portal.feedback.controller;

import com.smartstart.portal.feedback.dto.FeedbackDtos.CopyTopicsResult;
import com.smartstart.portal.feedback.dto.FeedbackDtos.FeedbackResponse;
import com.smartstart.portal.feedback.dto.FeedbackDtos.FeedbackRowResponse;
import com.smartstart.portal.feedback.dto.FeedbackDtos.UpsertFeedbackRequest;
import com.smartstart.portal.feedback.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;

@Tag(name = "Feedback")
@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService service;

    @Operation(summary = "All active students for the month (empty fields when not entered)")
    @GetMapping
    public List<FeedbackRowResponse> list(@Parameter(example = "2026-08") @RequestParam YearMonth month,
                                          @RequestParam(required = false) Long groupId) {
        return service.list(month, groupId);
    }

    @Operation(summary = "Create or update a student's feedback for the month")
    @PutMapping("/{studentId}")
    public FeedbackResponse upsert(@PathVariable Long studentId,
                                   @Parameter(example = "2026-08") @RequestParam YearMonth month,
                                   @Valid @RequestBody UpsertFeedbackRequest request) {
        return service.upsert(studentId, month, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Copy topics from the previous month for students without feedback this month")
    @PostMapping("/copy-topics")
    public CopyTopicsResult copyTopics(@Parameter(example = "2026-09") @RequestParam YearMonth month) {
        return service.copyTopicsFromPreviousMonth(month);
    }

    @Operation(summary = "Feedback history for one student")
    @GetMapping("/students/{studentId}")
    public List<FeedbackResponse> studentHistory(@PathVariable Long studentId) {
        return service.studentHistory(studentId);
    }
}
