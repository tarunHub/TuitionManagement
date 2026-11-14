package com.smartstart.portal.centre.entity;

import com.smartstart.portal.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** A tuition centre on the platform. Its staff see only its own students, classes, fees and messages. */
@Getter
@Setter
@Entity
@Table(name = "centre")
public class Centre extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    /** Student codes are this prefix plus a number, e.g. SS001. */
    @Column(name = "student_code_prefix", nullable = false, length = 10)
    private String studentCodePrefix;

    /** The number the next new student gets; never goes back, so codes are never reused. */
    @Column(name = "next_student_code", nullable = false)
    private long nextStudentCode = 1;

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Column(name = "contact_phone", length = 40)
    private String contactPhone;

    @Column(length = 300)
    private String address;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "mail_from", length = 150)
    private String mailFrom;

    @Column(name = "mail_from_name", length = 150)
    private String mailFromName;

    @Column(name = "smtp_host", length = 150)
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort;

    @Column(name = "smtp_username", length = 150)
    private String smtpUsername;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "smtp_password", length = 1000)
    private String smtpPassword;

    @Column(name = "smtp_auth", nullable = false)
    private boolean smtpAuth;

    @Column(name = "smtp_starttls", nullable = false)
    private boolean smtpStarttls;

    @Column(name = "sms_enabled", nullable = false)
    private boolean smsEnabled;

    @Column(name = "sms_gateway_url", length = 300)
    private String smsGatewayUrl;

    @Column(name = "sms_username", length = 150)
    private String smsUsername;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "sms_password", length = 1000)
    private String smsPassword;

    @Column(name = "sms_country_code", nullable = false, length = 5)
    private String smsCountryCode = "1";

    @Column(name = "messaging_imported", nullable = false)
    private boolean messagingImported;

    public boolean emailConfigured() {
        return notBlank(smtpHost) && smtpPort != null && notBlank(mailFrom);
    }

    public boolean smsConfigured() {
        return smsEnabled && notBlank(smsGatewayUrl);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
