package es.udc.fic.corpuslab.modules.project.guideline;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectSetupException;

@Component
public class GuidelinePdfValidator {

    private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    public void validate(byte[] pdfBytes, long maxGuidelinePdfSizeBytes) {
        if (pdfBytes == null || pdfBytes.length == 0 || pdfBytes.length > maxGuidelinePdfSizeBytes) {
            throw new InvalidProjectSetupException("Guideline PDF exceeds the maximum allowed size");
        }

        if (!hasPdfMagicHeader(pdfBytes)) {
            throw new InvalidProjectSetupException("Guideline file must be a valid PDF");
        }
    }

    private boolean hasPdfMagicHeader(byte[] bytes) {
        if (bytes.length < PDF_MAGIC.length) {
            return false;
        }

        for (int index = 0; index < PDF_MAGIC.length; index++) {
            if (bytes[index] != PDF_MAGIC[index]) {
                return false;
            }
        }
        return true;
    }
}
