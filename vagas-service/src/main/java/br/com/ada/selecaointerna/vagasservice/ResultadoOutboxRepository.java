package br.com.ada.selecaointerna.vagasservice;

import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ResultadoOutboxRepository extends JpaRepository<ResultadoOutbox, UUID> {
  List<ResultadoOutbox> findTop20ByPublicadoEmIsNullOrderByCriadoEmAsc();
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from ResultadoOutbox r where r.eventId = :id")
  Optional<ResultadoOutbox> bloquear(@Param("id") UUID id);
}
