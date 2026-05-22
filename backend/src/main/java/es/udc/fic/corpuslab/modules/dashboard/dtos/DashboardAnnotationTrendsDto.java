package es.udc.fic.corpuslab.modules.dashboard.dtos;

import java.util.List;

public record DashboardAnnotationTrendsDto(
        List<DashboardAnnotationTrendSeriesDto> series,
        List<DashboardAnnotationTrendDatumDto> data) {

    public DashboardAnnotationTrendsDto {
        series = series == null ? List.of() : List.copyOf(series);
        data = data == null ? List.of() : List.copyOf(data);
    }
}
