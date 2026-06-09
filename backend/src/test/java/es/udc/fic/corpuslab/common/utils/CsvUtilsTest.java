package es.udc.fic.corpuslab.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class CsvUtilsTest {

    @Test
    void parseRecordsShouldReturnEmptyListForNullOrEmptyInput() {
        assertThat(CsvUtils.parseRecords(null)).isEmpty();
        assertThat(CsvUtils.parseRecords("")).isEmpty();
    }

    @Test
    void parseTrimmedRecordShouldReturnFirstTrimmedRecord() {
        List<String> record = CsvUtils.parseTrimmedRecord(" one , two , three \nignored,row");

        assertThat(record).containsExactly("one", "two", "three");
    }

    @Test
    void printRecordsShouldProduceRoundTripSafeCsvWithoutTrailingNewline() {
        List<List<String>> records = List.of(
                List.of("name", "notes"),
                List.of("Alice", "hello, world"),
                List.of("Bob", "line1\nline2"));

        String csvContent = CsvUtils.printRecords(records);

        assertThat(csvContent).doesNotEndWith("\n");
        assertThat(CsvUtils.parseRecords(csvContent)).containsExactlyElementsOf(records);
    }

    @Test
    void printRecordShouldRenderSingleRecordCsv() {
        String csvLine = CsvUtils.printRecord(List.of("hello", "quoted, value"));

        assertThat(csvLine).isEqualTo("hello,\"quoted, value\"");
    }
}
