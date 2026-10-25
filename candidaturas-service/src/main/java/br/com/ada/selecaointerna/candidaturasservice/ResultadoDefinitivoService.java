package br.com.ada.selecaointerna.candidaturasservice;

import br.com.ada.selecaointerna.contracts.ResultadoPublicado;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @RequiredArgsConstructor
public class ResultadoDefinitivoService {
  private final ProcessoVagaRepository processos;
  private final ProcessoVagaService prontidao;
  private final RankingConsulta ranking;
  private final ObjectMapper mapper;

  @Transactional
  public void processar(ResultadoPublicado evento) {
    if (evento == null || evento.eventId() == null || evento.vagaId() == null || evento.ocorridoEm() == null
        || evento.vagaId().equals(ProcessoVagaService.BARREIRA)) {
      throw new IllegalArgumentException("Resultado sem identificadores/data validos");
    }
    var processo = processos.bloquear(evento.vagaId())
        .orElseThrow(() -> new IllegalArgumentException("Resultado recebido sem encerramento de inscricoes"));
    if (processo.getEventId() != null) {
      if (!evento.equals(ler(processo).publicacao())) throw new IllegalArgumentException("Publicacao divergente da original");
      return;
    }
    var estado = prontidao.consultar(evento.vagaId());
    if (!estado.apto()) throw new IllegalStateException("Ainda existem inscricoes sem avaliacao");
    var resultado = new ResultadoDefinitivoDto(evento, ranking.consultar(evento.vagaId()), estado.inscricoes(), estado.avaliacoes());
    try { processo.registrarResultado(evento.eventId(), evento.ocorridoEm(), mapper.writeValueAsString(resultado)); }
    catch (JsonProcessingException e) { throw new IllegalStateException("Falha ao serializar resultado definitivo", e); }
  }

  @Transactional(readOnly = true)
  public ResultadoDefinitivoDto consultar(UUID vagaId) {
    return ler(processos.findById(vagaId).filter(p -> p.getEventId() != null)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resultado definitivo ainda nao disponivel")));
  }

  private ResultadoDefinitivoDto ler(ProcessoVaga processo) {
    try { return mapper.readValue(processo.getResultadoJson(), ResultadoDefinitivoDto.class); }
    catch (JsonProcessingException e) { throw new IllegalStateException("Resultado persistido invalido", e); }
  }
}
