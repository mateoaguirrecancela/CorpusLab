package es.udc.fic.corpuslab.modules.project.annotationexport;

public record AnnotationExportColumn(
        Long userId,
        String annotationHeader,
        String commentHeader,
        boolean hasAnnotation,
        boolean hasComment) {
}
