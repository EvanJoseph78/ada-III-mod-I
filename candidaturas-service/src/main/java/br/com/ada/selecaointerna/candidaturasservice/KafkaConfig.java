package br.com.ada.selecaointerna.candidaturasservice;

import br.com.ada.selecaointerna.contracts.Topicos;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "mensageria.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig {
  @Bean
  NewTopic candidaturasRealizadas() {
    return TopicBuilder.name(Topicos.CANDIDATURAS_REALIZADAS).partitions(3).replicas(1).build();
  }
}
