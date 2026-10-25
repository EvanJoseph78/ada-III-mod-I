package br.com.ada.selecaointerna.candidaturasservice;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface CandidaturaRepository extends JpaRepository<Candidatura, UUID> {
  boolean existsByMatriculaEmpregadoAndIdVaga(Long matriculaEmpregado, Long idVaga);
  long countByIdVaga(Long idVaga);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Candidatura c where c.candidaturaId = :id")
  Optional<Candidatura> buscarParaAvaliar(@Param("id") UUID id);
}
