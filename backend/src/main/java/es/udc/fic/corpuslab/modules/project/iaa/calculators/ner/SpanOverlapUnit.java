package es.udc.fic.corpuslab.modules.project.iaa.calculators.ner;

public enum SpanOverlapUnit {
    CHARACTER,
    WORD;

    public static final String METADATA_KEY = "spanOverlapUnit";

    public static SpanOverlapUnit fromMetadata(Object value) {
        if (value == null) {
            return CHARACTER;
        }

        String normalizedValue = String.valueOf(value).trim().toUpperCase();
        if (normalizedValue.isEmpty() || normalizedValue.equals("CHAR") || normalizedValue.equals("CHARS")) {
            return CHARACTER;
        }
        if (normalizedValue.equals("WORD") || normalizedValue.equals("WORDS")) {
            return WORD;
        }

        return SpanOverlapUnit.valueOf(normalizedValue);
    }
}
