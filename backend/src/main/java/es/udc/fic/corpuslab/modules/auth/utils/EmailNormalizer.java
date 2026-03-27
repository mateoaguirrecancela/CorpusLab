package es.udc.fic.corpuslab.modules.auth.utils;

import java.util.Locale;

public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    public static String normalize(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static String canonicalizeGoogleEmail(String email) {
        String normalized = normalize(email);
        if (normalized == null || normalized.isBlank()) {
            return normalized;
        }

        int atIndex = normalized.indexOf('@');
        if (atIndex <= 0 || atIndex == normalized.length() - 1) {
            return normalized;
        }

        String localPart = normalized.substring(0, atIndex);
        String domainPart = normalized.substring(atIndex + 1);
        if (!"gmail.com".equals(domainPart) && !"googlemail.com".equals(domainPart)) {
            return normalized;
        }

        int plusIndex = localPart.indexOf('+');
        if (plusIndex >= 0) {
            localPart = localPart.substring(0, plusIndex);
        }

        localPart = localPart.replace(".", "");
        return localPart + "@gmail.com";
    }
}