package br.com.ada.selecaointerna.vagasservice;

import br.com.ada.selecaointerna.contracts.Topicos;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.*;

@Configuration @EnableScheduling @RequiredArgsConstructor
@ConditionalOnProperty(name = "mensageria.enabled", havingValue = "true", matchIfMissing = true)
public class ResultadoMensageria {
  private static final Logger log = LoggerFactory.getLogger(ResultadoMensageria.class);
  private final ResultadoOutboxRepository repository;
  private final ResultadoPublicador publicador;
  @Bean
  NewTopic resultadosPublicados() { return TopicBuilder.name(Topicos.VAGAS_RESULTADOS).partitions(3).replicas(1).build(); }
  @Scheduled(fixedDelayString = "${mensageria.outbox.intervalo-ms:1000}")
  public void publicarPendentes() {
    for (var evento : repository.findTop20ByPublicadoEmIsNullOrderByCriadoEmAsc()) {
      try { publicador.publicar(evento.getEventId()); }
      catch (RuntimeException e) {
        log.warn("Resultado {} permanece pendente: {}", evento.getEventId(), e.getClass().getSimpleName());
        break;
      }
    }
  }
}
