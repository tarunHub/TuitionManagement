package com.smartstart.portal.centre.service;

import com.smartstart.portal.centre.entity.Centre;
import com.smartstart.portal.centre.repository.CentreRepository;
import com.smartstart.portal.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * One-time move of the old single-centre email/SMS settings (.env) into the first centre, so it keeps sending exactly
 * as before. After that each centre's settings live in the app (Centre settings page).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MessagingImporter implements ApplicationRunner {

    private static final long FIRST_CENTRE = 1L;

    private final CentreRepository centres;
    private final AppProperties props;

    @Value("${spring.mail.host:}")
    private String smtpHost;
    @Value("${spring.mail.port:0}")
    private int smtpPort;
    @Value("${spring.mail.username:}")
    private String smtpUsername;
    @Value("${spring.mail.password:}")
    private String smtpPassword;
    @Value("${spring.mail.properties.mail.smtp.auth:false}")
    private boolean smtpAuth;
    @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}")
    private boolean smtpStarttls;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Centre c = centres.findById(FIRST_CENTRE).orElse(null);
        if (c == null || c.isMessagingImported()) {
            return;
        }
        if (notBlank(smtpHost) && !"localhost".equalsIgnoreCase(smtpHost)) {
            c.setSmtpHost(smtpHost);
            c.setSmtpPort(smtpPort > 0 ? smtpPort : null);
            c.setSmtpUsername(blankToNull(smtpUsername));
            c.setSmtpPassword(blankToNull(smtpPassword));
            c.setSmtpAuth(smtpAuth);
            c.setSmtpStarttls(smtpStarttls);
        } else if (notBlank(smtpHost)) {
            c.setSmtpHost(smtpHost);
            c.setSmtpPort(smtpPort > 0 ? smtpPort : null);
        }
        if (props.mail() != null) {
            c.setMailFrom(blankToNull(props.mail().from()));
            c.setMailFromName(blankToNull(props.mail().fromName()));
        }
        AppProperties.Sms sms = props.sms();
        if (sms != null) {
            c.setSmsEnabled(sms.enabled() && notBlank(sms.gatewayUrl()));
            c.setSmsGatewayUrl(blankToNull(sms.gatewayUrl()));
            c.setSmsUsername(blankToNull(sms.username()));
            c.setSmsPassword(blankToNull(sms.password()));
            if (notBlank(sms.defaultCountryCode())) {
                c.setSmsCountryCode(sms.defaultCountryCode().strip());
            }
        }
        c.setMessagingImported(true);
        centres.save(c);
        log.info("Copied the email/SMS settings from .env into centre '{}'", c.getName());
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String blankToNull(String s) {
        return notBlank(s) ? s.strip() : null;
    }
}
