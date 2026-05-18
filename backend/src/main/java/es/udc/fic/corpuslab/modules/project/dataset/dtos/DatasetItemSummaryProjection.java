package es.udc.fic.corpuslab.modules.project.dataset.dtos;

import java.time.Instant;

public interface DatasetItemSummaryProjection {

    Long getId();

    Integer getItemIndex();

    String getFileName();

    String getMimeType();

    Long getSizeBytes();

    Long getStepCount();

    Instant getCreatedAt();
}
