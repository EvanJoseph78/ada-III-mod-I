package br.com.ada.selecaointerna.candidaturasservice;

import java.time.Instant;
import java.util.UUID;

public record CandidaturaOutputDto(UUID candidaturaId, Long matriculaEmpregado, Long idVaga,
                                   Instant realizadaEm, String mensagem) {
  public CandidaturaOutputDto(Candidatura candidatura) {
    this(candidatura.getCandidaturaId(), candidatura.getMatriculaEmpregado(), candidatura.getIdVaga(),
        candidatura.getRealizadaEm(), "Candidatura registrada com sucesso.");
  }
}
