package es.udc.fic.corpuslab.modules.project.annotationexport;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class CsvAnnotationExportWriterTest {

    private final CsvAnnotationExportWriter writer = new CsvAnnotationExportWriter();

    @Test
    void appendCsvLineShouldJoinPlainValuesWithCommas() throws IOException {
        StringWriter target = new StringWriter();

        writer.appendCsvLine(target, Arrays.asList("id", "text", "label"));

        assertThat(target.toString()).isEqualTo("id,text,label\n");
    }

    @Test
    void appendCsvLineShouldQuoteAndEscapeValuesThatNeedIt() throws IOException {
        StringWriter target = new StringWriter();

        writer.appendCsvLine(target, Arrays.asList(
                "plain",
                "has,comma",
                "has\"quote",
                "has\nnewline",
                "has\rcarriage",
                null));

        assertThat(target.toString()).isEqualTo(
                "plain,\"has,comma\",\"has\"\"quote\",\"has\nnewline\",\"has\rcarriage\",\n");
    }

    @Test
    void appendCsvLineShouldHandleEmptyValuesList() throws IOException {
        StringWriter target = new StringWriter();

        writer.appendCsvLine(target, java.util.List.of());

        assertThat(target.toString()).isEqualTo("\n");
    }
}
