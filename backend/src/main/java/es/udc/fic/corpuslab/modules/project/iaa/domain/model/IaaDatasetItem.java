package es.udc.fic.corpuslab.modules.project.iaa.domain.model;

import java.util.List;
import java.util.Objects;

public record IaaDatasetItem(
        Long id,
        int itemIndex,
        List<IaaDatasetStep> steps,
        boolean csvDataset) {

    public IaaDatasetItem {
        steps = List.copyOf(Objects.requireNonNull(steps, "steps is required"));
    }
}
