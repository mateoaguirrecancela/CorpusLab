package es.udc.fic.corpuslab.modules.researchgroup.fixtures;

import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;

public class ResearchGroupTestBuilder {

    private String name = "Test Research Group";
    private String description = "A research group for testing purposes.";

    public static ResearchGroupTestBuilder validGroup() {
        return new ResearchGroupTestBuilder();
    }

    public ResearchGroupTestBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ResearchGroupTestBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public ResearchGroup build() {
        ResearchGroup group = new ResearchGroup();
        group.setName(name);
        group.setDescription(description);
        return group;
    }
}
