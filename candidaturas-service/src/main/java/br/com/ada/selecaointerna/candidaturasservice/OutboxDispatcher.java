package br.com.ada.selecaointerna.candidaturasservice;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mensageria.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxDispatcher {
  private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);
  private final OutboxRepository repository;
  private final PublicacaoOutboxService publicacao;

  @Scheduled(fixedDelayString = "${mensageria.outbox.intervalo-ms:1000}")
  public void publicarPendentes() {
    for (OutboxCandidatura evento : repository.findTop20ByPublicadoEmIsNullOrderByCriadoEmAsc()) {
      try { publicacao.publicar(evento.getEventId()); }
      catch (RuntimeException e) {
        log.warn("Evento {} permanece pendente: {}", evento.getEventId(), e.getClass().getSimpleName());
        break;
      }
    }
  }
}
