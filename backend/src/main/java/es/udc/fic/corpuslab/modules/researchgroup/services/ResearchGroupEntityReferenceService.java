package es.udc.fic.corpuslab.modules.researchgroup.services;

import org.springframework.stereotype.Service;

import es.udc.fic.corpuslab.modules.auth.entities.User;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroupInvitation;
import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroupMember;
import jakarta.persistence.EntityManager;

@Service
public class ResearchGroupEntityReferenceService {

    private final EntityManager entityManager;

    public ResearchGroupEntityReferenceService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void attachUser(ResearchGroupMember member, Long userId) {
        member.setUser(entityManager.getReference(User.class, userId));
    }

    public void attachInviterUser(ResearchGroupInvitation invitation, Long userId) {
        invitation.setInviterUser(entityManager.getReference(User.class, userId));
    }

    public void attachInvitedUser(ResearchGroupInvitation invitation, Long userId) {
        invitation.setInvitedUser(entityManager.getReference(User.class, userId));
    }
}
