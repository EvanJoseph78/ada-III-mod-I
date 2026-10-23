package br.com.ada.selecaointerna.candidaturasservice;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidaturaRepository extends JpaRepository<Candidatura, UUID> {}
