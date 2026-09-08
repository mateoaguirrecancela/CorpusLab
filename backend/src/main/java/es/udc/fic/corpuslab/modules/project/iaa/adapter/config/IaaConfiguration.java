package es.udc.fic.corpuslab.modules.project.iaa.adapter.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import es.udc.fic.corpuslab.modules.project.iaa.application.port.in.CalculateIaaMetricsUseCase;
import es.udc.fic.corpuslab.modules.project.iaa.application.service.CalculateIaaMetricsService;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.IaaMetricCalculator;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.classification.CohensKappaCalculator;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.classification.FleissKappaCalculator;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.classification.KrippendorffsAlphaCalculator;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.ner.SpanOverlapF1Calculator;
import es.udc.fic.corpuslab.modules.project.iaa.domain.calculators.xrr.XrrCalculator;
import es.udc.fic.corpuslab.modules.project.iaa.domain.registry.MetricsRegistry;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.classification.NominalAnnotationDataTransformer;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.ner.NerSpanAnnotationDataTransformer;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.xrr.XrrAnnotationDataTransformer;

@Configuration
public class IaaConfiguration {

    @Bean
    public NominalAnnotationDataTransformer nominalAnnotationDataTransformer() {
        return new NominalAnnotationDataTransformer();
    }

    @Bean
    public NerSpanAnnotationDataTransformer nerSpanAnnotationDataTransformer() {
        return new NerSpanAnnotationDataTransformer();
    }

    @Bean
    public XrrAnnotationDataTransformer xrrAnnotationDataTransformer() {
        return new XrrAnnotationDataTransformer();
    }

    @Bean
    public CohensKappaCalculator cohensKappaCalculator(NominalAnnotationDataTransformer transformer) {
        return new CohensKappaCalculator(transformer);
    }

    @Bean
    public FleissKappaCalculator fleissKappaCalculator(NominalAnnotationDataTransformer transformer) {
        return new FleissKappaCalculator(transformer);
    }

    @Bean
    public KrippendorffsAlphaCalculator krippendorffsAlphaCalculator(NominalAnnotationDataTransformer transformer) {
        return new KrippendorffsAlphaCalculator(transformer);
    }

    @Bean
    public SpanOverlapF1Calculator spanOverlapF1Calculator(NerSpanAnnotationDataTransformer transformer) {
        return new SpanOverlapF1Calculator(transformer);
    }

    @Bean
    public XrrCalculator xrrCalculator(XrrAnnotationDataTransformer transformer) {
        return new XrrCalculator(transformer);
    }

    @Bean
    public MetricsRegistry metricsRegistry(List<IaaMetricCalculator> calculators) {
        return new MetricsRegistry(calculators);
    }

    @Bean
    public CalculateIaaMetricsUseCase calculateIaaMetricsUseCase(MetricsRegistry metricsRegistry) {
        return new CalculateIaaMetricsService(metricsRegistry);
    }
}
