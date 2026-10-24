package br.com.ada.selecaointerna.candidaturasservice;

import java.time.Instant;
import java.util.UUID;

public record CandidaturaOutputDto(UUID candidaturaId, Long matriculaEmpregado, Long idVaga,
                                   Instant realizadaEm, String mensagem, String status,
                                   Boolean elegivel, Integer pontuacao) {
  public CandidaturaOutputDto(Candidatura candidatura) {
    this(candidatura, null);
  }

  public CandidaturaOutputDto(Candidatura candidatura, Avaliacao avaliacao) {
    this(candidatura.getCandidaturaId(), candidatura.getMatriculaEmpregado(), candidatura.getIdVaga(),
        candidatura.getRealizadaEm(), "Candidatura registrada com sucesso.",
        avaliacao == null ? "PENDENTE" : "AVALIADA", avaliacao == null ? null : avaliacao.isElegivel(),
        avaliacao == null ? null : avaliacao.getPontuacao());
  }
}
