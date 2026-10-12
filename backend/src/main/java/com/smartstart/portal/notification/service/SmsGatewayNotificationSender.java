package com.smartstart.portal.notification.service;

import com.smartstart.portal.config.AppProperties;
import com.smartstart.portal.notification.entity.Notification;
import com.smartstart.portal.notification.entity.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;

/**
 * Sends SMS through the Android "SMS Gateway" app (https://sms-gate.app): the texts go out from the centre's own phone
 * and plan. One request per notification; the app queues and sends each number separately, so parents never see each
 * other's numbers. The notification subject is only a title for the history list; just the body is texted.
 */
@Slf4j
@Component
public class SmsGatewayNotificationSender implements NotificationSender {

    private final AppProperties.Sms props;
    private final RestClient client;

    public SmsGatewayNotificationSender(AppProperties appProperties, RestClient.Builder builder) {
        this.props = appProperties.sms();
        Duration timeout = props.timeout() == null ? Duration.ofSeconds(15) : props.timeout();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        this.client = builder.requestFactory(factory).build();
    }

    /** True when SMS is switched on and a gateway URL is set; the UI hides the SMS option otherwise. */
    public boolean isConfigured() {
        return props.enabled() && props.gatewayUrl() != null && !props.gatewayUrl().isBlank();
    }

    public String defaultCountryCode() {
        return props.defaultCountryCode();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public void send(Notification notification) throws NotificationDeliveryException {
        if (!isConfigured()) {
            throw new NotificationDeliveryException("SMS is not configured (set SMS_ENABLED and SMS_GATEWAY_URL)", null);
        }
        List<String> numbers = notification.recipientList();
        if (numbers.isEmpty()) {
            throw new NotificationDeliveryException("No recipients", null);
        }
        record TextMessage(String text) {
        }
        record SendRequest(TextMessage textMessage, List<String> phoneNumbers) {
        }
        try {
            client.post()
                    .uri(endpoint(props.gatewayUrl()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> {
                        if (props.username() != null && !props.username().isBlank()) {
                            h.setBasicAuth(props.username(), props.password() == null ? "" : props.password());
                        }
                    })
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .body(new SendRequest(new TextMessage(notification.getBody()), numbers))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Queued SMS notification {} for {} recipient(s)", notification.getId(), numbers.size());
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            String detail = body.isBlank() ? "" : ": " + (body.length() > 300 ? body.substring(0, 300) : body);
            throw new NotificationDeliveryException("SMS gateway rejected the message (HTTP " + e.getStatusCode().value() + ")" + detail, e);
        } catch (Exception e) {
            String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            throw new NotificationDeliveryException("SMS gateway unreachable (is the phone on and the app's server running?): " + reason, e);
        }
    }

    /**
     * Accepts what the app screen shows, e.g. "10.0.0.12:8080": adds http:// when no scheme is given and the
     * local server's /message path when no path is given. Full URLs (including the cloud relay) are used as-is.
     */
    static String endpoint(String configured) {
        String url = configured.strip();
        if (!url.contains("://")) {
            url = "http://" + url;
        }
        int pathStart = url.indexOf('/', url.indexOf("://") + 3);
        if (pathStart < 0 || pathStart == url.length() - 1) {
            url = (pathStart < 0 ? url : url.substring(0, pathStart)) + "/message";
        }
        return url;
    }
}
