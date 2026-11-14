package com.smartstart.portal.centre.entity;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM encryption for passwords stored in the database (each centre's SMTP and SMS gateway passwords). The key is
 * APP_ENCRYPTION_KEY, or derived from JWT_SECRET when that isn't set. Changing the key makes stored passwords
 * unreadable, so they would need to be entered again.
 */
@Slf4j
@Component
public class SecretCipher {

    private static final String PREFIX = "enc:v1:";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static volatile SecretKeySpec key;

    public SecretCipher(@Value("${app.encryption-key:}") String encryptionKey, @Value("${app.jwt.secret:}") String jwtSecret) {
        String material = encryptionKey == null || encryptionKey.isBlank() ? jwtSecret : encryptionKey;
        if (material == null || material.isBlank()) {
            throw new IllegalStateException("APP_ENCRYPTION_KEY or JWT_SECRET must be set to store centre passwords");
        }
        if (encryptionKey == null || encryptionKey.isBlank()) {
            log.info("APP_ENCRYPTION_KEY not set; centre passwords are encrypted with a key derived from JWT_SECRET");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(("centre-secrets:" + material).getBytes(StandardCharsets.UTF_8));
            key = new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Could not set up encryption", e);
        }
    }

    static String encrypt(String plain) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, requireKey(), new GCMParameterSpec(128, iv));
            byte[] body = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + body.length).put(iv).put(body).array());
        } catch (Exception e) {
            throw new IllegalStateException("Could not encrypt a stored password", e);
        }
    }

    /** Returns null when the value can't be decrypted (for example after the key changed). */
    static String decrypt(String stored) {
        if (stored == null || stored.isEmpty()) {
            return stored;
        }
        if (!stored.startsWith(PREFIX)) {
            return stored;
        }
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, requireKey(), new GCMParameterSpec(128, all, 0, 12));
            return new String(c.doFinal(all, 12, all.length - 12), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("A stored centre password could not be decrypted (was APP_ENCRYPTION_KEY or JWT_SECRET changed?)");
            return null;
        }
    }

    private static SecretKeySpec requireKey() {
        if (key == null) {
            throw new IllegalStateException("SecretCipher not initialised");
        }
        return key;
    }
}
