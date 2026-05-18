package es.udc.fic.corpuslab.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

class FileSecurityUtilTest {

    @Test
    void sanitizeCsvShouldPreserveCsvStructureAndNeutralizeFormulaCells() {
        String csv = "name,formula\n\"Multi\nLine\",\"=SUM(A1:A2)\"\nNormal,\"safe, value\"";

        String sanitizedCsv = new String(
                FileSecurityUtil.sanitizeCsv(csv.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8);

        List<List<String>> sanitizedRecords = CsvUtils.parseRecords(sanitizedCsv);

        assertThat(sanitizedRecords).hasSize(3);
        assertThat(sanitizedRecords.get(1)).containsExactly("Multi\nLine", "'=SUM(A1:A2)");
        assertThat(sanitizedRecords.get(2)).containsExactly("Normal", "safe, value");
    }
}
