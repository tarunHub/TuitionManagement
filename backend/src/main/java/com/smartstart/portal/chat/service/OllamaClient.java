package com.smartstart.portal.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.smartstart.portal.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

/**
 * Minimal client for a local Ollama server's {@code /api/chat} (non-streaming, with tools). With
 * {@code LOGGING_LEVEL_COM_SMARTSTART_PORTAL_CHAT=DEBUG} every request and response body is logged in full.
 */
@Slf4j
@Component
public class OllamaClient {

    private final AppProperties.Chat props;
    private final ObjectMapper mapper;
    private final RestClient client;

    public OllamaClient(AppProperties appProperties, ObjectMapper mapper, RestClient.Builder builder) {
        this.props = appProperties.chat();
        this.mapper = mapper;
        Duration timeout = props.timeout() == null ? Duration.ofSeconds(180) : props.timeout();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(timeout);
        this.client = builder.requestFactory(factory).baseUrl(stripSlash(props.ollamaUrl())).build();
    }

    /** One chat round. Returns the assistant {@code message} object (with {@code content} and maybe {@code tool_calls}). */
    public ObjectNode chat(List<ObjectNode> messages, ArrayNode tools) {
        ObjectNode body = request(messages, 0);
        body.set("tools", tools);
        return send(body);
    }

    /**
     * One round without tools whose reply must match {@code schema} (Ollama structured outputs). Returns the parsed
     * JSON object the model wrote.
     */
    public JsonNode structured(List<ObjectNode> messages, JsonNode schema, double temperature) {
        ObjectNode body = request(messages, temperature);
        body.set("format", schema);
        String content = send(body).path("content").asText("");
        try {
            return mapper.readTree(content);
        } catch (Exception e) {
            throw new IllegalStateException("Ollama returned text that isn't the requested JSON");
        }
    }

    private ObjectNode request(List<ObjectNode> messages, double temperature) {
        ObjectNode body = mapper.createObjectNode();
        body.put("model", props.model());
        body.put("stream", false);
        // Reasoning mode makes small local models several times slower without helping simple lookups.
        body.put("think", false);
        body.putObject("options").put("temperature", temperature);
        body.putArray("messages").addAll(messages);
        return body;
    }

    private ObjectNode send(ObjectNode body) {
        if (log.isDebugEnabled()) {
            log.debug("Ollama request:\n{}", body.toPrettyString());
        }
        long started = System.currentTimeMillis();
        JsonNode response = client.post().uri("/api/chat").contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().body(JsonNode.class);
        if (log.isDebugEnabled()) {
            log.debug("Ollama response ({} ms):\n{}", System.currentTimeMillis() - started,
                    response == null ? "null" : response.toPrettyString());
        }
        if (response == null || !response.path("message").isObject()) {
            throw new IllegalStateException("Ollama returned no message");
        }
        return (ObjectNode) response.get("message");
    }

    /** True when Ollama answers and has the configured model pulled. */
    public boolean hasModel() {
        JsonNode tags = client.get().uri("/api/tags").retrieve().body(JsonNode.class);
        if (tags == null) {
            return false;
        }
        for (JsonNode m : tags.path("models")) {
            if (props.model().equalsIgnoreCase(m.path("name").asText()) || props.model().equalsIgnoreCase(m.path("model").asText())) {
                return true;
            }
        }
        return false;
    }

    public String model() {
        return props.model();
    }

    private static String stripSlash(String url) {
        String u = url == null || url.isBlank() ? "http://localhost:11434" : url.trim();
        return u.endsWith("/") ? u.substring(0, u.length() - 1) : u;
    }
}
