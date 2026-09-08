package es.udc.fic.corpuslab.modules.researchgroup.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;

public interface ResearchGroupRepository extends JpaRepository<ResearchGroup, Long> {

    Optional<ResearchGroup> findByInvitationCodeIgnoreCase(String invitationCode);
}
