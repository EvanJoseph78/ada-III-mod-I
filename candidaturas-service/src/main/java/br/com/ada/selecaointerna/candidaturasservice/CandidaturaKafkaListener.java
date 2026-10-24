package br.com.ada.selecaointerna.candidaturasservice;

import br.com.ada.selecaointerna.contracts.CandidaturaRealizada;
import br.com.ada.selecaointerna.contracts.Topicos;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mensageria.enabled", havingValue = "true", matchIfMissing = true)
public class CandidaturaKafkaListener {
  private final ObjectMapper mapper;
  private final AvaliacaoService service;

  @KafkaListener(id = "avaliacao-curricular", topics = Topicos.CANDIDATURAS_REALIZADAS,
      groupId = "${mensageria.grupo-avaliacao:avaliacao-curricular}")
  public void receber(String payload) {
    CandidaturaRealizada evento;
    try { evento = mapper.readValue(payload, CandidaturaRealizada.class); }
    catch (JsonProcessingException e) { throw new IllegalArgumentException("JSON de candidatura invalido"); }
    service.processar(evento);
  }
}
