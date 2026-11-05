package com.arvinder.banking.crypto;

import jakarta.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AES-256-GCM encryption for sensitive columns. Each value gets a fresh 12-byte IV, stored in
 * front of the ciphertext; GCM's tag detects tampering. Output is base64 so it fits a text column.
 */
@Component
public class AesGcmEncryptor {
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static volatile AesGcmEncryptor instance;

    private final SecureRandom random = new SecureRandom();
    private final SecretKey key;

    public AesGcmEncryptor(@Value("${security.encryption.key}") String base64Key) {
        byte[] raw = Base64.getDecoder().decode(base64Key);
        if (raw.length != 32) {
            throw new IllegalArgumentException("security.encryption.key must be 32 bytes (base64)");
        }
        this.key = new SecretKeySpec(raw, "AES");
    }

    @PostConstruct
    void register() {
        instance = this;
    }

    /** Used by the JPA converter, which Hibernate may instantiate outside Spring. */
    public static AesGcmEncryptor get() {
        if (instance == null) {
            throw new IllegalStateException("AesGcmEncryptor not initialised");
        }
        return instance;
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("encryption failed", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] all = Base64.getDecoder().decode(encoded);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, all, 0, IV_BYTES));
            byte[] pt = cipher.doFinal(all, IV_BYTES, all.length - IV_BYTES);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("decryption failed (wrong key or tampered data)", e);
        }
    }
}
