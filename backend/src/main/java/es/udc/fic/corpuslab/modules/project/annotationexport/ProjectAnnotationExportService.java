package es.udc.fic.corpuslab.modules.project.annotationexport;

import java.io.IOException;
import java.io.OutputStream;

public interface ProjectAnnotationExportService {

    String getAnnotationResultsCsvFileName(String authenticatedEmail, Long projectId);

    void writeAnnotationResultsCsv(String authenticatedEmail, Long projectId, OutputStream outputStream)
            throws IOException;
}
