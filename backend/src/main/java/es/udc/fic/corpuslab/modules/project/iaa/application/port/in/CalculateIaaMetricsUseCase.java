package es.udc.fic.corpuslab.modules.project.iaa.application.port.in;

public interface CalculateIaaMetricsUseCase {

    CalculateIaaMetricsResult calculate(CalculateIaaMetricsCommand command);
}
