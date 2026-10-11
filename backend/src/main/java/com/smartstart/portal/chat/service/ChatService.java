package com.smartstart.portal.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartstart.portal.chat.dto.ChatDtos.ChatMessage;
import com.smartstart.portal.chat.dto.ChatDtos.ChatResponse;
import com.smartstart.portal.chat.dto.ChatDtos.ChatRole;
import com.smartstart.portal.chat.dto.ChatDtos.ChatSource;
import com.smartstart.portal.chat.dto.ChatDtos.ChatStatusResponse;
import com.smartstart.portal.chat.service.ChatTools.ToolResult;
import com.smartstart.portal.common.exception.ApiException;
import com.smartstart.portal.common.exception.BadRequestException;
import com.smartstart.portal.common.exception.FeatureNotConfiguredException;
import com.smartstart.portal.common.util.AppTime;
import com.smartstart.portal.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Answers admin questions about the centre's data with a local Ollama model. The model can only call
 * {@link ChatTools} (read-only), and an answer given without any lookup is retried once with a reminder and then
 * flagged as ungrounded, because small models otherwise make up numbers.
 */
@Slf4j
@Service
public class ChatService {

    private static final String NO_LOOKUP_REMINDER = "You answered without calling a tool. Call the tool that holds this "
            + "information and answer from its result. If no tool covers the question, reply exactly: "
            + "\"I can only answer questions about the centre's students, fees, attendance, feedback and schedule.\"";

    private final AppProperties.Chat props;
    private final OllamaClient ollama;
    private final ChatTools tools;
    private final ObjectMapper mapper;
    private final Clock clock;

    public ChatService(AppProperties appProperties, OllamaClient ollama, ChatTools tools, ObjectMapper mapper, Clock clock) {
        this.props = appProperties.chat();
        this.ollama = ollama;
        this.tools = tools;
        this.mapper = mapper;
        this.clock = clock;
    }

    public ChatStatusResponse status() {
        if (!props.enabled()) {
            return new ChatStatusResponse(false, false, props.model(), "The assistant is off. Set CHAT_ENABLED=true in .env.");
        }
        try {
            return ollama.hasModel()
                    ? new ChatStatusResponse(true, true, props.model(), null)
                    : new ChatStatusResponse(true, false, props.model(), "Ollama is running but the model " + props.model()
                    + " is not installed. Run: ollama pull " + props.model());
        } catch (RestClientException e) {
            return new ChatStatusResponse(true, false, props.model(), "Can't reach Ollama at " + props.ollamaUrl()
                    + ". Is the Ollama app running?");
        }
    }

    public ChatResponse ask(List<ChatMessage> conversation) {
        if (!props.enabled()) {
            throw new FeatureNotConfiguredException("The assistant is off. Set CHAT_ENABLED=true in .env.");
        }
        if (conversation.getLast().role() != ChatRole.USER) {
            throw new BadRequestException("VALIDATION_FAILED", "The last message must be the user's question", "messages");
        }
        long started = System.currentTimeMillis();
        List<ObjectNode> messages = new ArrayList<>();
        messages.add(message("system", systemPrompt()));
        conversation.forEach(m -> messages.add(message(m.role() == ChatRole.USER ? "user" : "assistant", m.content())));
        ArrayNode definitions = tools.definitions();

        Map<String, ChatSource> sources = new LinkedHashMap<>();
        Set<String> toolsUsed = new LinkedHashSet<>();
        boolean reminded = false;
        int rounds = Math.max(props.maxRounds(), 2);
        for (int round = 0; round < rounds; round++) {
            ObjectNode reply = call(messages, definitions);
            messages.add(reply);
            JsonNode calls = reply.path("tool_calls");
            if (!calls.isArray() || calls.isEmpty()) {
                if (toolsUsed.isEmpty() && !reminded) {
                    reminded = true;
                    messages.add(message("system", NO_LOOKUP_REMINDER));
                    continue;
                }
                String answer = reply.path("content").asText("").strip();
                return new ChatResponse(answer.isEmpty() ? "Sorry, I couldn't put an answer together. Please rephrase the question." : answer,
                        List.copyOf(sources.values()), !toolsUsed.isEmpty(), List.copyOf(toolsUsed), props.model(),
                        System.currentTimeMillis() - started);
            }
            for (JsonNode c : calls) {
                String name = c.path("function").path("name").asText();
                JsonNode args = arguments(c.path("function").path("arguments"));
                ToolResult result = tools.run(name, args);
                toolsUsed.add(name);
                if (result.source() != null) {
                    sources.putIfAbsent(result.source().link(), result.source());
                }
                log.info("Chat tool {} {}", name, args);
                ObjectNode toolMessage = message("tool", toJson(result.data()));
                toolMessage.put("tool_name", name);
                messages.add(toolMessage);
            }
        }
        return new ChatResponse("Sorry, that took too many lookups. Try a more specific question (for example, name the month).",
                List.copyOf(sources.values()), !toolsUsed.isEmpty(), List.copyOf(toolsUsed), props.model(),
                System.currentTimeMillis() - started);
    }

    private ObjectNode call(List<ObjectNode> messages, ArrayNode definitions) {
        try {
            return ollama.chat(messages, definitions);
        } catch (ResourceAccessException e) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "ASSISTANT_UNAVAILABLE",
                    "The assistant didn't respond (is Ollama running at " + props.ollamaUrl() + "?)");
        } catch (RestClientException | IllegalStateException e) {
            log.warn("Ollama call failed: {}", e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ASSISTANT_ERROR", "The assistant failed: " + e.getMessage());
        }
    }

    private String systemPrompt() {
        LocalDate today = LocalDate.now(clock);
        YearMonth month = YearMonth.from(today);
        return """
                You are the assistant inside the Smart Start tutoring centre's admin portal. You answer the owner's questions \
                about the centre's own data: students, batches (class groups), fees and payments, attendance, monthly \
                feedback reports and the class schedule.

                Today is %s, so "this month" is %s (%s) and "last month" is %s.

                Rules:
                - Always call a tool to look the data up. Never guess or invent names, numbers or dates.
                - Pass months to tools as YYYY-MM.
                - Use the counts and lists the tools give you (for example studentsPerStatus, stillOwing, activePerBatch) \
                instead of counting yourself.
                - Money is in Canadian dollars; write amounts like $60.00. A positive balance means the family still owes; \
                a negative balance is a credit (they paid extra).
                - Fee status: PAID = paid in full, CREDIT = paid more than due, PARTIAL = paid some, UNPAID = nothing paid. \
                "Paid their fees" means PAID or CREDIT.
                - Answer briefly in plain English. Use a short bullet list when listing several students. Do not mention \
                tools, JSON or databases.
                - If the data doesn't contain the answer, say so plainly. You can only read data; you cannot change anything.
                """.formatted(today, AppTime.monthLabel(month), month, AppTime.monthLabel(month.minusMonths(1)));
    }

    private ObjectNode message(String role, String content) {
        return mapper.createObjectNode().put("role", role).put("content", content);
    }

    /** Ollama sends arguments as an object; some models send a JSON string instead. */
    private JsonNode arguments(JsonNode raw) {
        if (raw.isTextual()) {
            try {
                return mapper.readTree(raw.asText());
            } catch (Exception e) {
                return mapper.createObjectNode();
            }
        }
        return raw;
    }

    private String toJson(Object data) {
        try {
            return mapper.writeValueAsString(data);
        } catch (Exception e) {
            return "{\"error\":\"could not serialise result\"}";
        }
    }
}
