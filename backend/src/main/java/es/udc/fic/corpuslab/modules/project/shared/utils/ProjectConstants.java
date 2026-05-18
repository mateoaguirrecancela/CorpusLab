package es.udc.fic.corpuslab.modules.project.shared.utils;

public class ProjectConstants {
    public static final String NOT_MEMBER_ERROR = "User is not a member of this research group";
    public static final String CONTENT_KEY_FILE_NAME = "fileName";
    public static final String CONTENT_KEY_MIME_TYPE = "mimeType";
    public static final String CONTENT_KEY_SIZE_BYTES = "sizeBytes";
    public static final String CONTENT_KEY_STEP_COUNT = "stepCount";
    public static final String CONTENT_KEY_BASE64 = "base64";
    public static final String ANNOTATION_KEY_NOTES = "notes";
    public static final String NER_ANNOTATION_KEY_ENTITIES = "entities";
    public static final String NER_ANNOTATION_KEY_LABEL = "label";
    public static final String NER_ANNOTATION_KEY_TEXT = "text";
    public static final String NER_ANNOTATION_KEY_START_OFFSET = "startOffset";
    public static final String NER_ANNOTATION_KEY_END_OFFSET = "endOffset";
    public static final String ANNOTATION_KEY_BINARY_VALUE = "isExplanationCorrect";
    public static final String ANNOTATION_KEY_LABEL = "label";
    public static final String ANNOTATION_KEY_LABELS = "labels";
    public static final String ANNOTATION_KEY_TEXT = "text";
    public static final String ANNOTATION_WRAPPED_VALUE_KEY = "value";
    public static final String EXPORT_ANNOTATION_HEADER_SUFFIX = "_annotation";
    public static final String EXPORT_COMMENT_HEADER_SUFFIX = "_coment";
    public static final int DEFAULT_ANNOTATION_STEPS_LIMIT = 50;
    public static final int MAX_ANNOTATION_STEPS_LIMIT = 250;
    public static final int PREVIEW_MAX_LENGTH = 160;

    private ProjectConstants() {
    }
}
