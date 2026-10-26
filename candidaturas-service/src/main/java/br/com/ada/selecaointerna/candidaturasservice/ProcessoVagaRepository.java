package br.com.ada.selecaointerna.candidaturasservice;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProcessoVagaRepository extends JpaRepository<ProcessoVaga, UUID> {
  boolean existsByIdVaga(Long idVaga);
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from ProcessoVaga p where p.vagaId = :id")
  Optional<ProcessoVaga> bloquear(@Param("id") UUID id);
}
