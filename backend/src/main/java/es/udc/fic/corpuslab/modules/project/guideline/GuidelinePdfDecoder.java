package es.udc.fic.corpuslab.modules.project.guideline;

import org.springframework.stereotype.Component;

import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectSetupException;
import es.udc.fic.corpuslab.modules.project.shared.utils.ProjectDatasetUtils;

@Component
public class GuidelinePdfDecoder {

    public byte[] decode(String rawGuidelinePdfBase64) {
        String normalizedBase64 = StringUtils.trimToNull(rawGuidelinePdfBase64);
        if (normalizedBase64 == null) {
            return new byte[0];
        }

        try {
            return ProjectDatasetUtils.decodeStoredBase64(normalizedBase64);
        } catch (IllegalArgumentException ex) {
            throw new InvalidProjectSetupException("Guideline PDF content is not valid Base64");
        }
    }

    public long calculateSizeBytes(String rawGuidelinePdfBase64) {
        String normalizedBase64 = StringUtils.trimToNull(rawGuidelinePdfBase64);
        if (normalizedBase64 == null) {
            return 0L;
        }

        String compactBase64 = ProjectDatasetUtils.normalizeStoredBase64(normalizedBase64)
                .replaceAll("\\s+", "");
        if (compactBase64.isEmpty()) {
            return 0L;
        }

        int padding = 0;
        if (compactBase64.endsWith("==")) {
            padding = 2;
        } else if (compactBase64.endsWith("=")) {
            padding = 1;
        }

        return Math.max(0L, ((long) compactBase64.length() * 3L / 4L) - padding);
    }
}
