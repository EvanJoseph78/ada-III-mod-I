package br.com.ada.selecaointerna.candidaturasservice;

import br.com.ada.selecaointerna.contracts.Topicos;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicacaoOutboxService {
  private final OutboxRepository repository;
  private final KafkaTemplate<String, String> kafka;

  @Transactional
  public void publicar(UUID eventId) {
    OutboxCandidatura evento = repository.buscarParaPublicar(eventId).orElseThrow();
    if (evento.getPublicadoEm() != null) return;
    try {
      kafka.send(Topicos.CANDIDATURAS_REALIZADAS, evento.getCandidaturaId().toString(), evento.getPayload())
          .get(12, TimeUnit.SECONDS);
      evento.confirmarPublicacao();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Publicacao interrompida; evento permanece pendente", e);
    } catch (Exception e) {
      throw new IllegalStateException("Publicacao falhou; evento permanece pendente", e);
    }
  }
}
