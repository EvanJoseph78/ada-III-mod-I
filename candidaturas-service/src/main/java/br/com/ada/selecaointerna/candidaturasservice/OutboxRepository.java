package br.com.ada.selecaointerna.candidaturasservice;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<OutboxCandidatura, UUID> {
  List<OutboxCandidatura> findTop20ByPublicadoEmIsNullOrderByCriadoEmAsc();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from OutboxCandidatura o where o.eventId = :id")
  Optional<OutboxCandidatura> buscarParaPublicar(@Param("id") UUID id);
}
