package es.udc.fic.corpuslab.modules.project.utils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import es.udc.fic.corpuslab.common.utils.StringUtils;
import es.udc.fic.corpuslab.modules.project.entities.DatasetItem;
import es.udc.fic.corpuslab.modules.project.exceptions.InvalidProjectDatasetException;

public class ProjectDatasetUtils {

    private ProjectDatasetUtils() {
    }

    public static boolean isCsvDatasetItem(DatasetItem datasetItem) {
        String mimeType = valueAsString(datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_MIME_TYPE)).toLowerCase();
        String fileName = valueAsString(datasetItem.getContent().get(ProjectConstants.CONTENT_KEY_FILE_NAME)).toLowerCase();

        return isCsvFile(fileName, mimeType);
    }

    public static boolean isCsvFile(String fileName, String mimeType) {
        String lowerFileName = fileName != null ? fileName.toLowerCase() : "";
        String lowerMimeType = mimeType != null ? mimeType.toLowerCase() : "";
        return lowerMimeType.contains("csv") || lowerFileName.endsWith(".csv");
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

        String[] rawLines = csvContent.split("\\r?\\n", -1);
        List<String> lines = List.of(rawLines);
        if (lines.isEmpty()) {
            return new CsvDatasetContent(List.of(), List.of());
        }

        String header = removeUtf8Bom(lines.get(0)).trim();
        List<String> rawHeaderColumns = parseCsvColumns(header);
        List<DatasetStepData> steps = new ArrayList<>();

        for (int lineIndex = 1; lineIndex < lines.size(); lineIndex++) {
            String row = lines.get(lineIndex);
            if (row == null || row.isBlank()) {
                continue;
            }

            String normalizedRow = row.trim();
            List<String> rowColumns = parseCsvColumns(normalizedRow);
            List<String> normalizedHeaders = normalizeCsvHeaderColumns(rawHeaderColumns, rowColumns.size());
            Map<String, String> rowValues = toCsvRowValues(normalizedHeaders, rowColumns);

            if (header.isBlank()) {
                steps.add(new DatasetStepData(truncatePreview(normalizedRow), rowValues));
            } else {
                steps.add(new DatasetStepData(buildCsvStepPreview(header, normalizedRow), rowValues));
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

        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean insideQuotes = false;

        for (int index = 0; index < line.length(); index++) {
            char currentChar = line.charAt(index);

            if (currentChar == '"') {
                if (insideQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    currentValue.append('"');
                    index++;
                } else {
                    insideQuotes = !insideQuotes;
                }
                continue;
            }

            if (currentChar == ',' && !insideQuotes) {
                values.add(currentValue.toString().trim());
                currentValue = new StringBuilder();
                continue;
            }

            currentValue.append(currentChar);
        }

        values.add(currentValue.toString().trim());
        return values;
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
