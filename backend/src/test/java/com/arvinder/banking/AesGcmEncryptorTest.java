package com.arvinder.banking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.arvinder.banking.crypto.AesGcmEncryptor;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class AesGcmEncryptorTest {
    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private final AesGcmEncryptor enc = new AesGcmEncryptor(KEY);

    @Test
    void roundTrips() {
        assertThat(enc.decrypt(enc.encrypt("4000123456789012"))).isEqualTo("4000123456789012");
    }

    @Test
    void sameInputEncryptsDifferentlyEachTime() {
        assertThat(enc.encrypt("same")).isNotEqualTo(enc.encrypt("same"));
    }

    @Test
    void tamperedCiphertextIsRejected() {
        byte[] raw = Base64.getDecoder().decode(enc.encrypt("secret"));
        raw[raw.length - 1] ^= 1;
        String tampered = Base64.getEncoder().encodeToString(raw);
        assertThatThrownBy(() -> enc.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsWrongKeyLength() {
        assertThatThrownBy(() -> new AesGcmEncryptor(Base64.getEncoder().encodeToString(new byte[16])))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
