package es.udc.fic.corpuslab.modules.project.shared.utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.udc.fic.corpuslab.common.utils.CsvUtils;
import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.project.shared.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.shared.exceptions.InvalidProjectDatasetException;

public class ProjectDatasetUtils {

    private static final Set<String> NER_SUPPORTED_EXTENSIONS = Set.of("txt", "json", "csv");

    private ProjectDatasetUtils() {
    }

    public static boolean isCsvDatasetItem(DatasetItem datasetItem) {
        String mimeType = valueAsString(datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_MIME_TYPE));
        String fileName = valueAsString(datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_FILE_NAME));

        return isCsvFile(fileName, mimeType);
    }

    public static boolean isCsvFile(String fileName, String mimeType) {
        String lowerFileName = fileName == null ? "" : fileName.trim().toLowerCase();
        String lowerMimeType = mimeType == null ? "" : mimeType.trim().toLowerCase();
        return lowerMimeType.contains("csv") || lowerFileName.endsWith(".csv");
    }

    public static boolean isNerCompatibleDatasetItem(DatasetItem datasetItem) {
        String mimeType = valueAsString(datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_MIME_TYPE));
        String fileName = valueAsString(datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_FILE_NAME));

        return isNerCompatibleFile(fileName, mimeType);
    }

    public static boolean isNerCompatibleFile(String fileName, String mimeType) {
        String normalizedMimeType = mimeType == null ? "" : mimeType.trim().toLowerCase();
        String extension = extractFileExtension(fileName);

        return normalizedMimeType.equals("text/plain")
                || normalizedMimeType.equals("application/json")
                || normalizedMimeType.equals("text/json")
                || normalizedMimeType.endsWith("+json")
                || normalizedMimeType.contains("csv")
                || NER_SUPPORTED_EXTENSIONS.contains(extension);
    }

    public static DatasetStepDefinition resolveStepDefinition(DatasetItem datasetItem) {
        if (isCsvDatasetItem(datasetItem)) {
            CsvDatasetContent parsedCsv = parseCsvDatasetContent(datasetItem);
            return new DatasetStepDefinition(parsedCsv.steps());
        }

        String fileName = valueAsString(datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_FILE_NAME));
        if (fileName.isBlank()) {
            fileName = "Dataset item #" + (datasetItem.getItemIndex() + 1);
        }

        return new DatasetStepDefinition(List.of(new DatasetStepData(fileName, null)));
    }

    public static CsvDatasetContent parseCsvDatasetContent(DatasetItem datasetItem) {
        String base64Content = valueAsString(datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_BASE64));
        if (base64Content.isBlank()) {
            return new CsvDatasetContent(List.of(), List.of());
        }

        String csvContent;
        try {
            csvContent = new String(decodeStoredBase64(base64Content), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new InvalidProjectDatasetException("CSV dataset item has invalid Base64 content");
        }

        List<List<String>> records;
        try {
            records = CsvUtils.parseTrimmedRecords(csvContent);
        } catch (IllegalArgumentException ex) {
            throw new InvalidProjectDatasetException("CSV dataset item has invalid CSV content");
        }

        if (records.isEmpty()) {
            return new CsvDatasetContent(List.of(), List.of());
        }

        boolean hasHeader = hasNonBlankFirstPhysicalLine(csvContent);
        List<String> rawHeaderColumns = hasHeader ? normalizeParsedHeaderColumns(records.getFirst()) : List.of();
        String headerPreview = hasHeader ? CsvUtils.printRecord(rawHeaderColumns) : "";
        List<DatasetStepData> steps = new ArrayList<>();

        for (int recordIndex = 1; recordIndex < records.size(); recordIndex++) {
            List<String> rowColumns = records.get(recordIndex);
            if (isBlankCsvRecord(rowColumns)) {
                continue;
            }

            List<String> normalizedHeaders = normalizeCsvHeaderColumns(rawHeaderColumns, rowColumns.size());
            Map<String, String> rowValues = toCsvRowValues(normalizedHeaders, rowColumns);
            String rowPreview = CsvUtils.printRecord(rowColumns);

            if (!hasHeader) {
                steps.add(new DatasetStepData(truncatePreview(rowPreview), rowValues));
            } else {
                steps.add(new DatasetStepData(buildCsvStepPreview(headerPreview, rowPreview), rowValues));
            }
        }

        List<String> headerColumns = normalizeCsvHeaderColumns(rawHeaderColumns, rawHeaderColumns.size());
        return new CsvDatasetContent(headerColumns, steps);
    }

    public static List<String> parseCsvHeaderColumns(DatasetItem datasetItem) {
        CsvDatasetContent csvDatasetContent = parseCsvDatasetContent(datasetItem);
        return csvDatasetContent.headers();
    }

    public static List<String> normalizeCsvHeaderColumns(List<String> rawHeaderColumns, int minColumns) {
        int requiredColumns = Math.max(minColumns, rawHeaderColumns.size());
        List<String> normalizedHeaders = new ArrayList<>(requiredColumns);
        Set<String> seen = new LinkedHashSet<>();

        for (int index = 0; index < requiredColumns; index++) {
            String candidate = index < rawHeaderColumns.size()
                    ? StringUtils.trimToNull(rawHeaderColumns.get(index))
                    : null;

            if (candidate == null) {
                candidate = "column_" + (index + 1);
            }

            String normalizedCandidate = candidate;
            int duplicateIndex = 2;
            while (!seen.add(normalizedCandidate.toLowerCase())) {
                normalizedCandidate = candidate + "_" + duplicateIndex++;
            }

            normalizedHeaders.add(normalizedCandidate);
        }

        return normalizedHeaders;
    }

    public static Map<String, String> toCsvRowValues(List<String> headers, List<String> rowColumns) {
        if (headers.isEmpty()) {
            return Map.of();
        }

        Map<String, String> rowValues = new LinkedHashMap<>();
        for (int index = 0; index < headers.size(); index++) {
            String value = index < rowColumns.size() ? rowColumns.get(index) : "";
            rowValues.put(headers.get(index), value);
        }

        return rowValues;
    }

    public static List<String> parseCsvColumns(String line) {
        if (line == null || line.isBlank()) {
            return List.of();
        }

        return CsvUtils.parseTrimmedRecord(line);
    }

    public static String buildCsvStepPreview(String header, String row) {
        return truncatePreview(header) + "\n" + truncatePreview(row);
    }

    public static String removeUtf8Bom(String value) {
        if (value.startsWith("\uFEFF")) {
            return value.substring(1);
        }

        return value;
    }

    public static String truncatePreview(String value) {
        if (value.length() <= ProjectConstants.PREVIEW_MAX_LENGTH) {
            return value;
        }

        return value.substring(0, ProjectConstants.PREVIEW_MAX_LENGTH - 3) + "...";
    }

    public static byte[] decodeStoredBase64(String rawBase64) {
        String normalizedBase64 = normalizeStoredBase64(rawBase64);
        return Base64.getMimeDecoder().decode(normalizedBase64);
    }

    public static String normalizeStoredBase64(String rawBase64) {
        String trimmedBase64 = rawBase64 == null ? "" : rawBase64.trim();
        if (trimmedBase64.isEmpty()) {
            return trimmedBase64;
        }

        if (trimmedBase64.regionMatches(true, 0, "data:", 0, 5)) {
            int markerIndex = trimmedBase64.toLowerCase().indexOf("base64,");
            if (markerIndex >= 0) {
                return trimmedBase64.substring(markerIndex + "base64,".length()).trim();
            }
        }

        return trimmedBase64;
    }

    public static String describeDatasetItem(DatasetItem datasetItem) {
        String fileName = valueAsString(datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_FILE_NAME)).trim();
        String normalizedFileName = fileName.isBlank()
                ? "dataset-item-" + datasetItem.getItemIndex()
                : fileName;
        String mimeType = valueAsString(datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_MIME_TYPE)).trim();

        return mimeType.isBlank() ? normalizedFileName : normalizedFileName + " (" + mimeType + ")";
    }

    public static String extractFileExtension(String fileName) {
        String normalizedFileName = fileName == null ? "" : fileName.trim().toLowerCase();
        int lastDotIndex = normalizedFileName.lastIndexOf('.');

        if (lastDotIndex < 0 || lastDotIndex == normalizedFileName.length() - 1) {
            return "";
        }

        return normalizedFileName.substring(lastDotIndex + 1);
    }

    public static String valueAsString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static boolean hasNonBlankFirstPhysicalLine(String csvContent) {
        String[] firstLineParts = csvContent.split("\\R", 2);
        String firstLine = firstLineParts.length == 0 ? "" : removeUtf8Bom(firstLineParts[0]).trim();

        return !firstLine.isBlank();
    }

    private static List<String> normalizeParsedHeaderColumns(List<String> headerColumns) {
        List<String> normalizedHeaders = new ArrayList<>(headerColumns.size());

        for (int index = 0; index < headerColumns.size(); index++) {
            String value = headerColumns.get(index);
            String normalizedValue = index == 0 ? removeUtf8Bom(value).trim() : value.trim();
            normalizedHeaders.add(normalizedValue);
        }

        return normalizedHeaders;
    }

    private static boolean isBlankCsvRecord(List<String> record) {
        return record == null || record.stream().allMatch(value -> value == null || value.isBlank());
    }

    public record DatasetStepDefinition(List<DatasetStepData> steps) {
        public int totalSteps() {
            return steps.size();
        }

        public String previewForStep(int stepIndex) {
            if (stepIndex >= 0 && stepIndex < steps.size()) {
                return steps.get(stepIndex).preview();
            }

            return "Step " + (stepIndex + 1);
        }

        public Map<String, String> rowValuesForStep(int stepIndex) {
            if (stepIndex >= 0 && stepIndex < steps.size()) {
                return steps.get(stepIndex).rowValues();
            }

            return null;
        }
    }

    public record DatasetStepData(
            String preview,
            Map<String, String> rowValues) {
    }

    public record CsvDatasetContent(
            List<String> headers,
            List<DatasetStepData> steps) {
    }
}
