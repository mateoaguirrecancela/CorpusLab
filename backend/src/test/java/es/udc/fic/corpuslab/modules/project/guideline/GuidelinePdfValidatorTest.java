package es.udc.fic.corpuslab.modules.project.guideline;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectSetupException;

class GuidelinePdfValidatorTest {

    private final GuidelinePdfValidator validator = new GuidelinePdfValidator();

    @Test
    void validateShouldAcceptValidPdfMagicHeaderWithinSizeLimit() {
        byte[] pdfBytes = "%PDF-1.7\nbody".getBytes(StandardCharsets.UTF_8);

        assertThatCode(() -> validator.validate(pdfBytes, pdfBytes.length + 10L))
                .doesNotThrowAnyException();
    }

    @Test
    void validateShouldRejectEmptyPayload() {
        assertThatThrownBy(() -> validator.validate(new byte[0], 32L))
                .isInstanceOf(InvalidProjectSetupException.class)
                .hasMessageContaining("maximum allowed size");
    }

    @Test
    void validateShouldRejectPayloadLargerThanLimit() {
        byte[] pdfBytes = "%PDF-1.7\nbody".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> validator.validate(pdfBytes, pdfBytes.length - 1L))
                .isInstanceOf(InvalidProjectSetupException.class)
                .hasMessageContaining("maximum allowed size");
    }

    @Test
    void validateShouldRejectPayloadWithoutPdfMagicHeader() {
        byte[] notPdfBytes = "plain text".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> validator.validate(notPdfBytes, 64L))
                .isInstanceOf(InvalidProjectSetupException.class)
                .hasMessageContaining("valid PDF");
    }
}
