package es.udc.fic.corpuslab.modules.researchgroup.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import es.udc.fic.corpuslab.modules.researchgroup.entities.ResearchGroup;

public interface ResearchGroupRepository extends JpaRepository<ResearchGroup, Long> {
}
