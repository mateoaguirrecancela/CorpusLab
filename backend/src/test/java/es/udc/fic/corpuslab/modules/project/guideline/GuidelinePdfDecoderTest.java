package es.udc.fic.corpuslab.modules.project.guideline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectSetupException;

class GuidelinePdfDecoderTest {

    private final GuidelinePdfDecoder decoder = new GuidelinePdfDecoder();

    @Test
    void decodeShouldReturnEmptyByteArrayWhenInputIsNullOrBlank() {
        assertThat(decoder.decode(null)).isEmpty();
        assertThat(decoder.decode("   ")).isEmpty();
    }

    @Test
    void decodeShouldHandleDataUrlPrefixAndWhitespace() {
        byte[] pdfBytes = "%PDF-1.7\nbody".getBytes(StandardCharsets.UTF_8);
        String encoded = Base64.getEncoder().encodeToString(pdfBytes);
        String raw = "  data:application/pdf;base64,\n" + encoded + " \n ";

        assertThat(decoder.decode(raw)).isEqualTo(pdfBytes);
    }

    @Test
    void decodeShouldRejectInvalidBase64Content() {
        assertThatThrownBy(() -> decoder.decode("not-base64%%%"))
                .isInstanceOf(InvalidProjectSetupException.class)
                .hasMessageContaining("not valid Base64");
    }

    @Test
    void calculateSizeBytesShouldReturnExpectedDecodedLength() {
        byte[] pdfBytes = "%PDF-1.7\nbody".getBytes(StandardCharsets.UTF_8);
        String encoded = Base64.getEncoder().encodeToString(pdfBytes);
        String raw = " data:application/pdf;base64,\n" + encoded + "\n";

        assertThat(decoder.calculateSizeBytes(raw)).isEqualTo(pdfBytes.length);
    }

    @Test
    void calculateSizeBytesShouldReturnZeroWhenInputIsBlank() {
        assertThat(decoder.calculateSizeBytes("  ")).isZero();
        assertThat(decoder.calculateSizeBytes(null)).isZero();
    }
}
