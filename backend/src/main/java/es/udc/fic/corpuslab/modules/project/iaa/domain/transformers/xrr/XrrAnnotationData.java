package es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.xrr;

import java.util.List;

import es.udc.fic.corpuslab.modules.project.shared.enums.ProjectType;
import es.udc.fic.corpuslab.modules.project.iaa.domain.transformers.TransformedAnnotationData;

public record XrrAnnotationData(
        ProjectType projectType,
        List<List<String>> groupXRows,
        List<List<String>> groupYRows,
        List<String> groupXSources,
        List<String> groupYSources,
        int candidateUnitCount,
        int skippedUnitCount,
        int totalGroupXSourceCount,
        int totalGroupYSourceCount) implements TransformedAnnotationData {

    public XrrAnnotationData {
        groupXRows = immutableRows(groupXRows);
        groupYRows = immutableRows(groupYRows);
        groupXSources = groupXSources == null ? List.of() : List.copyOf(groupXSources);
        groupYSources = groupYSources == null ? List.of() : List.copyOf(groupYSources);
        totalGroupXSourceCount = Math.max(totalGroupXSourceCount, groupXSources.size());
        totalGroupYSourceCount = Math.max(totalGroupYSourceCount, groupYSources.size());
    }

    public XrrAnnotationData(
            ProjectType projectType,
            List<List<String>> groupXRows,
            List<List<String>> groupYRows,
            List<String> groupXSources,
            List<String> groupYSources,
            int candidateUnitCount,
            int skippedUnitCount) {
        this(
                projectType,
                groupXRows,
                groupYRows,
                groupXSources,
                groupYSources,
                candidateUnitCount,
                skippedUnitCount,
                groupXSources == null ? 0 : groupXSources.size(),
                groupYSources == null ? 0 : groupYSources.size());
    }

    @Override
    public int annotatorCount() {
        return groupXSources.size() + groupYSources.size();
    }

    @Override
    public int unitCount() {
        return Math.min(groupXRows.size(), groupYRows.size());
    }

    public int groupXRaterCount() {
        return groupXSources.size();
    }

    public int groupYRaterCount() {
        return groupYSources.size();
    }

    public int totalAnnotatorCount() {
        return totalGroupXSourceCount + totalGroupYSourceCount;
    }

    private static List<List<String>> immutableRows(List<List<String>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return rows.stream()
                .map(row -> row == null ? List.<String>of() : List.copyOf(row))
                .toList();
    }
}
