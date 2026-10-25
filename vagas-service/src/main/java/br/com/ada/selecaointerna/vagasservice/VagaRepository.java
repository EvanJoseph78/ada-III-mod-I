package br.com.ada.selecaointerna.vagasservice;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface VagaRepository extends JpaRepository<Vaga, Long> {
  Optional<Vaga> findByVagaId(UUID id);
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select v from Vaga v where v.vagaId = :id")
  Optional<Vaga> bloquear(@Param("id") UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select v from Vaga v where v.idVaga = :id")
  Optional<Vaga> bloquearPorId(@Param("id") Long id);
}
