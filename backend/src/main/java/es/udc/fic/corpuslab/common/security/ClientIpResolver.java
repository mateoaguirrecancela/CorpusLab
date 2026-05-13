package es.udc.fic.corpuslab.common.security;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String forwardedFor = firstHeaderValue(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            return forwardedFor;
        }

        String realIp = firstHeaderValue(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }

        String forwarded = request.getHeader("Forwarded");
        if (forwarded != null) {
            String forwardedIp = parseForwardedFor(forwarded);
            if (forwardedIp != null) {
                return forwardedIp;
            }
        }

        return request.getRemoteAddr();
    }

    private static String firstHeaderValue(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        String firstValue = header.split(",", 2)[0].trim();
        return firstValue.isBlank() ? null : firstValue;
    }

    private static String parseForwardedFor(String header) {
        for (String part : header.split(";")) {
            String trimmed = part.trim();
            if (trimmed.regionMatches(true, 0, "for=", 0, 4)) {
                String value = trimmed.substring(4).trim();
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
                    value = value.substring(1, value.length() - 1);
                }
                return value.isBlank() ? null : value;
            }
        }
        return null;
    }
}
