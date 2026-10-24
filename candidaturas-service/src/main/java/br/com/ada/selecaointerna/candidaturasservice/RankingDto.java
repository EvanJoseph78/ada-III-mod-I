package br.com.ada.selecaointerna.candidaturasservice;

import java.util.List;
import java.util.UUID;

public record RankingDto(UUID vagaId, List<Classificado> classificados) {
  public RankingDto { classificados = List.copyOf(classificados); }

  public record Classificado(int posicao, UUID candidaturaId, UUID empregadoId, Long matriculaEmpregado,
                             int pontuacao, java.time.Instant inscritaEm) {}
}
