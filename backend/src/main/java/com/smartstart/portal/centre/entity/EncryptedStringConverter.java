package com.smartstart.portal.centre.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Stores a string column encrypted (see {@link SecretCipher}). */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return SecretCipher.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return SecretCipher.decrypt(dbData);
    }
}
