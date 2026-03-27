package es.udc.fic.corpuslab.modules.auth.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmailNormalizerTest {

    @Test
    void canonicalizeGoogleEmailShouldNormalizeDotsAndPlusAlias() {
        String canonical = EmailNormalizer.canonicalizeGoogleEmail("  Na.Me+tag@GoogleMail.com ");

        assertThat(canonical).isEqualTo("name@gmail.com");
    }

    @Test
    void canonicalizeGoogleEmailShouldKeepNonGoogleDomainsUntouchedExceptNormalization() {
        String canonical = EmailNormalizer.canonicalizeGoogleEmail("  User+tag@Example.com ");

        assertThat(canonical).isEqualTo("user+tag@example.com");
    }

    @Test
    void normalizeShouldReturnNullWhenInputIsNull() {
        assertThat(EmailNormalizer.normalize(null)).isNull();
    }
}
