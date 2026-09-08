package es.udc.fic.corpuslab.modules.project.annotationexport;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class CsvAnnotationExportWriter {

    public void appendCsvLine(Writer writer, List<String> values) throws IOException {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                writer.append(',');
            }
            writer.append(escapeCsvValue(values.get(index)));
        }
        writer.append('\n');
    }

    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }
        boolean mustBeQuoted = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        if (!mustBeQuoted) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
