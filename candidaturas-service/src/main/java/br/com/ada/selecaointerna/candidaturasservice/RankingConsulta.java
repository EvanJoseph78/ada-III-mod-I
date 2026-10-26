package br.com.ada.selecaointerna.candidaturasservice;

import java.util.UUID;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RankingConsulta {
  private final AvaliacaoRepository repository;

  @Transactional(readOnly = true)
  public RankingDto consultar(UUID vagaId) {
    var linhas = repository.buscarRanking(vagaId);
    return new RankingDto(vagaId, IntStream.range(0, linhas.size()).mapToObj(i -> {
      var linha = linhas.get(i);
      return new RankingDto.Classificado(i + 1, linha.candidaturaId(), linha.empregadoId(),
          linha.matriculaEmpregado(), linha.pontuacao(), linha.inscritaEm());
    }).toList());
  }
}
