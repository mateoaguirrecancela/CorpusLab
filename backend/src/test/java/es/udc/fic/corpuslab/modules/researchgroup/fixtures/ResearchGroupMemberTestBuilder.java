package es.udc.fic.corpuslab.modules.researchgroup.fixtures;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroupMember;
import es.udc.fic.corpuslab.modules.researchgroup.enums.ResearchGroupMemberRole;

public class ResearchGroupMemberTestBuilder {

    private ResearchGroupMemberRole role = ResearchGroupMemberRole.ANNOTATOR;
    private User user;
    private ResearchGroup researchGroup;

    public static ResearchGroupMemberTestBuilder validMember() {
        return new ResearchGroupMemberTestBuilder();
    }

    public ResearchGroupMemberTestBuilder withRole(ResearchGroupMemberRole role) {
        this.role = role;
        return this;
    }

    public ResearchGroupMemberTestBuilder withUser(User user) {
        this.user = user;
        return this;
    }

    public ResearchGroupMemberTestBuilder withResearchGroup(ResearchGroup researchGroup) {
        this.researchGroup = researchGroup;
        return this;
    }

    public ResearchGroupMember build() {
        ResearchGroupMember member = new ResearchGroupMember();
        member.setRole(role);
        member.setUser(user);
        member.setResearchGroup(researchGroup);
        return member;
    }
}
