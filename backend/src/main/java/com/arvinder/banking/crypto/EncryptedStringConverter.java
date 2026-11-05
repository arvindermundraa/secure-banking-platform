package com.arvinder.banking.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Encrypts a String column on write and decrypts it on read. */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : AesGcmEncryptor.get().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AesGcmEncryptor.get().decrypt(dbData);
    }
}
