package es.udc.fic.corpuslab.modules.project.shared.utils;

import es.udc.fic.corpuslab.common.utils.StringUtils;

public class ProjectCommonUtils {

    private ProjectCommonUtils() {
    }

    public static String normalizeHexColor(String value) {
        String trimmed = StringUtils.trimToNull(value);
        return trimmed == null ? null : trimmed.toUpperCase();
    }
}
