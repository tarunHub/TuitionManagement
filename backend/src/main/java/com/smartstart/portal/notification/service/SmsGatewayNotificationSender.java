package com.smartstart.portal.notification.service;

import com.smartstart.portal.centre.entity.Centre;
import com.smartstart.portal.centre.service.CurrentCentre;
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

    private final CurrentCentre currentCentre;
    private final RestClient client;

    public SmsGatewayNotificationSender(AppProperties appProperties, CurrentCentre currentCentre, RestClient.Builder builder) {
        this.currentCentre = currentCentre;
        Duration timeout = appProperties.sms() == null || appProperties.sms().timeout() == null
                ? Duration.ofSeconds(15) : appProperties.sms().timeout();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        this.client = builder.requestFactory(factory).build();
    }

    /** True when the current centre has SMS switched on and a gateway URL set; the UI hides the SMS option otherwise. */
    public boolean isConfigured() {
        return currentCentre.get().smsConfigured();
    }

    public String defaultCountryCode() {
        return currentCentre.get().getSmsCountryCode();
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public void send(Notification notification) throws NotificationDeliveryException {
        Centre props = currentCentre.get();
        if (!props.smsConfigured()) {
            throw new NotificationDeliveryException("SMS is not set up for this centre (Centre settings → SMS)", null);
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
                    .uri(endpoint(props.getSmsGatewayUrl()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(h -> {
                        if (props.getSmsUsername() != null && !props.getSmsUsername().isBlank()) {
                            h.setBasicAuth(props.getSmsUsername(), props.getSmsPassword() == null ? "" : props.getSmsPassword());
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
