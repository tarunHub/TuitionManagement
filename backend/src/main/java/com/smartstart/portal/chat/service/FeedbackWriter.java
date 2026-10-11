package com.smartstart.portal.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartstart.portal.attendance.dto.AttendanceDtos.MonthlyAttendanceSummary;
import com.smartstart.portal.attendance.service.AttendanceService;
import com.smartstart.portal.chat.dto.ChatDtos.FeedbackDraftRequest;
import com.smartstart.portal.chat.dto.ChatDtos.FeedbackDraftResponse;
import com.smartstart.portal.common.exception.ApiException;
import com.smartstart.portal.common.exception.BadRequestException;
import com.smartstart.portal.common.exception.FeatureNotConfiguredException;
import com.smartstart.portal.common.util.AppTime;
import com.smartstart.portal.config.AppProperties;
import com.smartstart.portal.student.dto.StudentDtos.StudentResponse;
import com.smartstart.portal.student.service.StudentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.time.YearMonth;
import java.util.List;

/**
 * Turns the admin's rough notes into a parent-ready monthly feedback report (topics taught, improvement areas,
 * achievements) with the local Ollama model. It only suggests wording: the admin reviews it and saves the form.
 */
@Slf4j
@Service
public class FeedbackWriter {

    private static final String SYSTEM_PROMPT = """
            You write the monthly progress feedback that the Smart Start tutoring centre sends to parents.
            You get facts about one student and the tutor's rough notes, and you return three fields:
            - topicsTaught: what was covered this month, as a short comma-separated list or one sentence.
            - improvementAreas: what the student should work on, as 1-3 kind, specific, encouraging sentences.
            - achievements: what went well, as 1-3 warm, specific sentences.

            Rules:
            - Every point in the tutor's notes must appear in one of the fields (problems and missed classes go in \
            improvementAreas, praise in achievements).
            - Use ONLY the facts and notes given. Never invent topics, marks, test results or behaviour.
            - Keep every point the tutor already wrote (reword it, never drop it); fix spelling and grammar and make it \
            read well for parents. Start each field with a capital letter.
            - Write in plain Canadian English, third person. Refer to the student by first name, never "he" or "she" \
            (you don't know the student's pronouns). No greetings or sign-offs.
            - Mention attendance only if the tutor's notes or existing text mention it.
            - If there is nothing to say for a field, return an empty string for it.
            """;

    private final AppProperties.Chat props;
    private final OllamaClient ollama;
    private final StudentService students;
    private final AttendanceService attendance;
    private final ObjectMapper mapper;

    public FeedbackWriter(AppProperties appProperties, OllamaClient ollama, StudentService students,
                          AttendanceService attendance, ObjectMapper mapper) {
        this.props = appProperties.chat();
        this.ollama = ollama;
        this.students = students;
        this.attendance = attendance;
        this.mapper = mapper;
    }

    public FeedbackDraftResponse draft(FeedbackDraftRequest request) {
        if (!props.enabled()) {
            throw new FeatureNotConfiguredException("The AI assistant is off. Set CHAT_ENABLED=true in .env.");
        }
        if (blank(request.notes()) && blank(request.topicsTaught()) && blank(request.improvementAreas())
                && blank(request.achievements())) {
            throw new BadRequestException("VALIDATION_FAILED", "Write a few notes first", "notes");
        }
        long started = System.currentTimeMillis();
        StudentResponse student = students.get(request.studentId());
        YearMonth month = YearMonth.parse(request.month());

        List<ObjectNode> messages = List.of(
                message("system", SYSTEM_PROMPT),
                message("user", facts(student, month, request)));
        JsonNode out = call(messages);
        return new FeedbackDraftResponse(text(out, "topicsTaught"), text(out, "improvementAreas"), text(out, "achievements"),
                props.model(), System.currentTimeMillis() - started);
    }

    private String facts(StudentResponse s, YearMonth month, FeedbackDraftRequest r) {
        StringBuilder b = new StringBuilder();
        b.append("Student: ").append(s.fullName());
        if (!blank(s.grade())) {
            b.append(", grade ").append(s.grade());
        }
        if (!blank(s.classGroupName())) {
            b.append(", batch ").append(s.classGroupName());
        }
        if (s.subjects() != null && !s.subjects().isEmpty()) {
            b.append(", subjects: ").append(String.join(", ", s.subjects()));
        }
        b.append("\nMonth: ").append(AppTime.monthLabel(month));
        attendance.history(s.id()).months().stream().filter(m -> month.toString().equals(m.month())).findFirst()
                .filter(m -> m.held() > 0)
                .ifPresent((MonthlyAttendanceSummary m) -> b.append("\nAttendance this month: present ").append(m.present())
                        .append(" of ").append(m.held()).append(" classes"));
        b.append("\n\nTutor's notes:\n").append(orNone(r.notes()));
        b.append("\n\nAlready written (improve and keep the facts):");
        b.append("\ntopicsTaught: ").append(orNone(r.topicsTaught()));
        b.append("\nimprovementAreas: ").append(orNone(r.improvementAreas()));
        b.append("\nachievements: ").append(orNone(r.achievements()));
        return b.toString();
    }

    private JsonNode call(List<ObjectNode> messages) {
        ObjectNode schema = mapper.createObjectNode().put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        List<String> fields = List.of("topicsTaught", "improvementAreas", "achievements");
        fields.forEach(f -> properties.putObject(f).put("type", "string"));
        fields.forEach(schema.putArray("required")::add);
        try {
            return ollama.structured(messages, schema, 0.3);
        } catch (ResourceAccessException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ASSISTANT_UNAVAILABLE",
                    "The assistant didn't respond (is Ollama running at " + props.ollamaUrl() + "?)");
        } catch (RestClientException | IllegalStateException e) {
            log.warn("Ollama feedback draft failed: {}", e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ASSISTANT_ERROR", "The assistant failed: " + e.getMessage());
        }
    }

    private ObjectNode message(String role, String content) {
        return mapper.createObjectNode().put("role", role).put("content", content);
    }

    private static String text(JsonNode out, String field) {
        String v = out.path(field).asText("").strip();
        if (!v.isEmpty()) {
            v = Character.toUpperCase(v.charAt(0)) + v.substring(1);
        }
        return v.length() > 8000 ? v.substring(0, 8000) : v;
    }

    private static String orNone(String s) {
        return blank(s) ? "(none)" : s.strip();
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
