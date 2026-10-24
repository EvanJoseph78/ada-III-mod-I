package br.com.ada.selecaointerna.candidaturasservice;

import br.com.ada.selecaointerna.contracts.Topicos;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.time.Duration;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "mensageria.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig {
  @Bean
  NewTopic candidaturasRealizadas() {
    return TopicBuilder.name(Topicos.CANDIDATURAS_REALIZADAS).partitions(3).replicas(1).build();
  }

  @Bean
  NewTopic candidaturasInvalidas() {
    return TopicBuilder.name(Topicos.CANDIDATURAS_REALIZADAS + ".DLT").partitions(3).replicas(1).build();
  }

  @Bean
  DefaultErrorHandler avaliacaoErrorHandler(KafkaTemplate<String, String> kafka,
      @Value("${mensageria.retry.intervalo-ms:1000}") long intervalo) {
    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafka,
        (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
    recoverer.setFailIfSendResultIsError(true);
    recoverer.setWaitForSendResultTimeout(Duration.ofSeconds(12));
    ExponentialBackOffWithMaxRetries backoff = new ExponentialBackOffWithMaxRetries(2);
    backoff.setInitialInterval(intervalo);
    backoff.setMultiplier(2.0);
    backoff.setMaxInterval(Math.max(5000, intervalo));
    DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backoff);
    handler.addNotRetryableExceptions(IllegalArgumentException.class);
    return handler;
  }

  @Bean
  ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
      ConcurrentKafkaListenerContainerFactoryConfigurer configurer, ConsumerFactory<Object, Object> consumerFactory,
      DefaultErrorHandler avaliacaoErrorHandler) {
    var factory = new ConcurrentKafkaListenerContainerFactory<Object, Object>();
    configurer.configure(factory, consumerFactory);
    factory.setCommonErrorHandler(avaliacaoErrorHandler);
    return factory;
  }
}
