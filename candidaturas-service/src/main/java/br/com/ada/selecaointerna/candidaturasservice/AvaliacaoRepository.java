package br.com.ada.selecaointerna.candidaturasservice;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, UUID> {}
