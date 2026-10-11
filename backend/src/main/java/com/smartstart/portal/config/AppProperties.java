package com.smartstart.portal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Cors cors,
        Admin admin,
        Login login,
        Mail mail,
        Jobs jobs,
        Google google,
        Sms sms,
        Chat chat
) {

    public record Jwt(String secret, Duration expiration) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    /** Seed admin created on first startup; values come from ADMIN_USERNAME / ADMIN_PASSWORD. */
    public record Admin(String username, String password, String fullName) {
    }

    public record Login(int maxFailures, Duration lockout) {
    }

    public record Mail(String from, String fromName) {
    }

    public record Jobs(Job classReminder, Job feeReminder, Job dispatch) {
    }

    public record Job(boolean enabled, String cron) {
    }

    public record Google(boolean enabled, String credentialsPath, String applicationName, boolean sendInvites) {
    }

    /**
     * Android "SMS Gateway" app (sms-gate.app). {@code gatewayUrl} is the full send endpoint: the phone's local server
     * ({@code http://<phone-ip>:8080/message}) or the cloud relay ({@code https://api.sms-gate.app/3rdparty/v1/messages}).
     * Numbers without a leading + get {@code defaultCountryCode} prepended.
     */
    /**
     * "Ask" chatbot backed by a local Ollama model. It only reads data through {@code ChatTools}; nothing leaves the
     * machine. {@code maxRounds} caps tool-call round trips per question.
     */
    public record Chat(boolean enabled, String ollamaUrl, String model, Duration timeout, int maxRounds) {
    }

    public record Sms(boolean enabled, String gatewayUrl, String username, String password, String defaultCountryCode,
                      Duration timeout) {
    }
}
