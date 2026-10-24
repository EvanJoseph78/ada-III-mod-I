package br.com.ada.selecaointerna.candidaturasservice;

import br.com.ada.selecaointerna.contracts.CandidaturaRealizada;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AvaliacaoService {
  private final CandidaturaRepository candidaturas;
  private final AvaliacaoRepository avaliacoes;
  private final AvaliadorCurricular avaliador;
  private final ObjectMapper mapper;

  @Transactional
  public void processar(CandidaturaRealizada evento) {
    if (evento == null || evento.eventId() == null || evento.candidaturaId() == null) {
      throw new IllegalArgumentException("Evento sem identificadores");
    }
    // O evento e interno ao servico. A linha existe antes da publicacao pela outbox.
    Candidatura candidatura = candidaturas.buscarParaAvaliar(evento.candidaturaId())
        .orElseThrow(() -> new IllegalArgumentException("Candidatura do evento inexistente"));
    if (candidatura.getEventoCandidatura() == null) {
      throw new IllegalArgumentException("Candidatura historica sem snapshots");
    }
    try {
      CandidaturaRealizada esperado = mapper.readValue(candidatura.getEventoCandidatura(), CandidaturaRealizada.class);
      if (!esperado.equals(evento)) throw new IllegalArgumentException("Evento diverge do snapshot registrado");
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Snapshot registrado invalido", e);
    }
    // O bloqueio pessimista da candidatura coordena tambem entregas concorrentes.
    if (avaliacoes.existsById(evento.candidaturaId())) return;
    avaliacoes.saveAndFlush(new Avaliacao(evento, avaliador.avaliar(evento.perfilSnapshot(), evento.criteriosSnapshot())));
  }
}
