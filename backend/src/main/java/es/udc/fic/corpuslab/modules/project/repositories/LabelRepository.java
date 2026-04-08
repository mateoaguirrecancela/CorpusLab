package es.udc.fic.corpuslab.modules.project.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import es.udc.fic.corpuslab.modules.project.entities.Label;

public interface LabelRepository extends JpaRepository<Label, Long> {
}
