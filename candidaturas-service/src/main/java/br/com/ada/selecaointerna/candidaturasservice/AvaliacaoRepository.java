package br.com.ada.selecaointerna.candidaturasservice;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AvaliacaoRepository extends JpaRepository<Avaliacao, UUID> {
  long countByVagaId(UUID vagaId);

  @Query("""
      select new br.com.ada.selecaointerna.candidaturasservice.RankingLinha(
          a.candidaturaId, a.empregadoId, c.matriculaEmpregado, a.pontuacao, c.realizadaEm)
      from Avaliacao a join Candidatura c on c.candidaturaId = a.candidaturaId
      where a.vagaId = :vagaId and a.elegivel = true
      order by a.pontuacao desc, c.realizadaEm asc, a.candidaturaId asc
      """)
  List<RankingLinha> buscarRanking(@Param("vagaId") UUID vagaId);
}
