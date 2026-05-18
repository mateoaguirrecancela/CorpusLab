package es.udc.fic.corpuslab.common.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

public final class CsvUtils {

    private static final CSVFormat CSV_READ_FORMAT = CSVFormat.RFC4180;
    private static final CSVFormat TRIMMED_CSV_READ_FORMAT = CSVFormat.RFC4180.withTrim();
    private static final CSVFormat CSV_WRITE_FORMAT = CSVFormat.RFC4180.withRecordSeparator("\n");

    private CsvUtils() {
    }

    public static List<List<String>> parseRecords(String csvContent) {
        return parseRecords(csvContent, CSV_READ_FORMAT);
    }

    public static List<List<String>> parseTrimmedRecords(String csvContent) {
        return parseRecords(csvContent, TRIMMED_CSV_READ_FORMAT);
    }

    public static List<String> parseTrimmedRecord(String csvContent) {
        List<List<String>> records = parseTrimmedRecords(csvContent);
        return records.isEmpty() ? List.of() : records.getFirst();
    }

    public static String printRecord(List<String> columns) {
        return printRecords(List.of(columns));
    }

    public static String printRecords(List<List<String>> records) {
        StringWriter writer = new StringWriter();

        try (CSVPrinter printer = new CSVPrinter(writer, CSV_WRITE_FORMAT)) {
            for (List<String> record : records) {
                printer.printRecord(record);
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not write CSV content", ex);
        }

        return removeTrailingRecordSeparator(writer.toString());
    }

    private static List<List<String>> parseRecords(String csvContent, CSVFormat csvFormat) {
        if (csvContent == null || csvContent.isEmpty()) {
            return List.of();
        }

        try (CSVParser parser = CSVParser.parse(csvContent, csvFormat)) {
            List<List<String>> records = new ArrayList<>();

            for (CSVRecord record : parser) {
                List<String> values = new ArrayList<>(record.size());
                record.forEach(values::add);
                records.add(values);
            }

            return records;
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid CSV content", ex);
        }
    }

    private static String removeTrailingRecordSeparator(String value) {
        return value.endsWith("\n") ? value.substring(0, value.length() - 1) : value;
    }
}
