package br.com.ada.selecaointerna.vagasservice;

import br.com.ada.selecaointerna.contracts.ResultadoPublicado;
import br.com.ada.selecaointerna.contracts.Topicos;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class ResultadoPublicador {
  private final ResultadoOutboxRepository repository;
  private final KafkaTemplate<String, String> kafka;
  private final ObjectMapper mapper;

  @Transactional
  public ResultadoPublicado registrar(UUID vagaId) {
    var evento = new ResultadoPublicado(UUID.randomUUID(), Instant.now(), vagaId);
    try { repository.saveAndFlush(new ResultadoOutbox(evento, mapper.writeValueAsString(evento))); }
    catch (JsonProcessingException e) { throw new IllegalStateException("Falha ao serializar resultado", e); }
    return evento;
  }

  @Transactional
  public void publicar(UUID eventId) {
    var evento = repository.bloquear(eventId).orElseThrow();
    if (evento.getPublicadoEm() != null) return;
    try {
      kafka.send(Topicos.VAGAS_RESULTADOS, evento.getVagaId().toString(), evento.getPayload()).get(12, TimeUnit.SECONDS);
      evento.confirmar();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Publicacao interrompida; resultado permanece pendente", e);
    } catch (Exception e) { throw new IllegalStateException("Envio falhou; resultado permanece pendente", e); }
  }
}
