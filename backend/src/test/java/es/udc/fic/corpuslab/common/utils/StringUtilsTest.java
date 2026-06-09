package es.udc.fic.corpuslab.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StringUtilsTest {

    @Test
    void trimToNullShouldReturnNullForNullAndBlankValues() {
        assertThat(StringUtils.trimToNull(null)).isNull();
        assertThat(StringUtils.trimToNull("   ")).isNull();
    }

    @Test
    void trimToNullShouldReturnTrimmedValueWhenNotBlank() {
        assertThat(StringUtils.trimToNull("  corpuslab  ")).isEqualTo("corpuslab");
    }
}
