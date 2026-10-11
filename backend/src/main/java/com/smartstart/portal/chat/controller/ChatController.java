package com.smartstart.portal.chat.controller;

import com.smartstart.portal.chat.dto.ChatDtos.ChatRequest;
import com.smartstart.portal.chat.dto.ChatDtos.FeedbackDraftRequest;
import com.smartstart.portal.chat.dto.ChatDtos.FeedbackDraftResponse;
import com.smartstart.portal.chat.dto.ChatDtos.ChatResponse;
import com.smartstart.portal.chat.dto.ChatDtos.ChatStatusResponse;
import com.smartstart.portal.chat.service.ChatService;
import com.smartstart.portal.chat.service.FeedbackWriter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Assistant")
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService service;
    private final FeedbackWriter feedbackWriter;

    @Operation(summary = "Ask a question about the centre's data (read-only, answered by the local Ollama model)")
    @PostMapping
    public ChatResponse ask(@Valid @RequestBody ChatRequest request) {
        return service.ask(request.messages());
    }

    @Operation(summary = "Whether the assistant is enabled and Ollama is reachable")
    @GetMapping("/status")
    public ChatStatusResponse status() {
        return service.status();
    }

    @Operation(summary = "Suggest parent-ready wording for a student's monthly feedback from rough notes (nothing is saved)")
    @PostMapping("/feedback-draft")
    public FeedbackDraftResponse draftFeedback(@Valid @RequestBody FeedbackDraftRequest request) {
        return feedbackWriter.draft(request);
    }
}
