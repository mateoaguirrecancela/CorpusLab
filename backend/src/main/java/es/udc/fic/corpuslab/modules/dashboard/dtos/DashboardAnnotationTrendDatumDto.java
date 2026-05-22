package es.udc.fic.corpuslab.modules.dashboard.dtos;

import java.time.LocalDate;
import java.util.Map;

public record DashboardAnnotationTrendDatumDto(
        LocalDate isoDate,
        Map<String, Long> values) {

    public DashboardAnnotationTrendDatumDto {
        values = values == null ? Map.of() : Map.copyOf(values);
    }
}
