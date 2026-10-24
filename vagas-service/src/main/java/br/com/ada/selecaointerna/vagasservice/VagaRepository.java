package br.com.ada.selecaointerna.vagasservice;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VagaRepository extends JpaRepository<Vaga, Long> {
  Optional<Vaga> findByVagaId(UUID id);
}
