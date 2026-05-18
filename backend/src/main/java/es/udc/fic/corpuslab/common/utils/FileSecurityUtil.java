package es.udc.fic.corpuslab.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.apache.tika.Tika;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectDatasetException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class FileSecurityUtil {

    private static final Tika TIKA = new Tika();

    // Dangerous extensions that should NEVER be allowed
    private static final Set<String> BANNED_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "sh", "php", "jsp", "asp", "aspx", "js", "vbs", "jar", "war", "ear", "bin");

    // Whitelist of common safe extensions (can be expanded)
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "txt", "pdf", "csv", "json", "png", "jpg", "jpeg", "gif", "doc", "docx", "xls", "xlsx");

    // CSV Injection characters
    private static final List<String> CSV_INJECTION_CHARS = List.of("=", "+", "-", "@");

    private FileSecurityUtil() {
    }

    /**
     * Validates a file against various security threats.
     */
    public static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidProjectDatasetException("File is empty or null");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new InvalidProjectDatasetException("Filename is missing");
        }

        // 1. Anti-Path Traversal & Filename Sanitization
        validateFilename(originalFilename);

        // 2. MIME Spoofing detection with Magic Bytes
        validateMagicBytes(file);
    }

    /**
     * Validates that the filename doesn't contain path traversal sequences and has
     * a safe extension.
     */
    private static void validateFilename(String filename) {
        String cleanName = StringUtils.cleanPath(filename);
        if (cleanName.contains("..")) {
            log.warn("Path traversal attempt detected in filename: {}", filename);
            throw new InvalidProjectDatasetException("Invalid filename: potential path traversal");
        }

        String extension = StringUtils.getFilenameExtension(cleanName);
        if (extension == null || extension.isBlank()) {
            log.warn("File without extension: {}", filename);
            throw new InvalidProjectDatasetException("File must have an extension");
        }

        String lowExtension = extension.toLowerCase();
        if (BANNED_EXTENSIONS.contains(lowExtension)) {
            log.warn("Banned extension detected: {}", lowExtension);
            throw new InvalidProjectDatasetException("Forbidden file type: " + extension);
        }
    }

    /**
     * Uses Apache Tika to detect the actual content type via magic bytes.
     */
    private static void validateMagicBytes(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            String detectedMimeType = TIKA.detect(is);
            String declaredMimeType = file.getContentType();

            log.debug("File: {}, Declared: {}, Detected: {}", file.getOriginalFilename(), declaredMimeType,
                    detectedMimeType);

            // Basic check for executable/dangerous formats detected by Tika
            if (detectedMimeType.equals("application/x-msdownload") ||
                    detectedMimeType.equals("application/x-sh") ||
                    detectedMimeType.equals("application/x-executable") ||
                    detectedMimeType.equals("application/x-sharedlib")) {
                log.warn("Dangerous binary format detected: {}", detectedMimeType);
                throw new InvalidProjectDatasetException("Binary/Executable files are not allowed");
            }

            // We could also check if detectedMimeType matches declaredMimeType if we want
            // to be stricter
        } catch (IOException e) {
            log.error("Error reading file for magic bytes validation", e);
            throw new InvalidProjectDatasetException("Could not validate file content");
        }
    }

    /**
     * Sanitizes CSV content to prevent formula injection.
     */
    public static byte[] sanitizeCsv(byte[] content) {
        String csv = new String(content, StandardCharsets.UTF_8);
        if (csv.isBlank()) {
            return content;
        }

        try {
            List<List<String>> sanitizedRecords = CsvUtils.parseRecords(csv).stream()
                    .map(record -> record.stream().map(FileSecurityUtil::sanitizeCell).toList())
                    .toList();

            return CsvUtils.printRecords(sanitizedRecords).getBytes(StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new InvalidProjectDatasetException("Invalid CSV content");
        }
    }

    private static String sanitizeCell(String cell) {
        if (cell == null || cell.isBlank())
            return cell;

        String checkValue = cell.trim();

        if (CSV_INJECTION_CHARS.stream().anyMatch(checkValue::startsWith)) {
            return "'" + cell;
        }
        return cell;
    }
}
