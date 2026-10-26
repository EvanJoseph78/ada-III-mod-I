package br.com.ada.selecaointerna.vagasservice;

import br.com.ada.selecaointerna.contracts.Topicos;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest @ActiveProfiles("test")
class ResultadoOutboxTest {
  @Autowired ResultadoPublicador publicador;
  @Autowired ResultadoOutboxRepository repository;
  @Autowired PlatformTransactionManager transactions;
  @MockitoBean KafkaTemplate<String, String> kafka;

  @Test
  void ackConfirmaEnvioERegistroConfirmadoNaoReenvia() {
    var evento = publicador.registrar(UUID.randomUUID());
    when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
    publicador.publicar(evento.eventId());
    assertThat(repository.findById(evento.eventId()).orElseThrow().getPublicadoEm()).isNotNull();
    publicador.publicar(evento.eventId());
    verify(kafka, times(1)).send(eq(Topicos.VAGAS_RESULTADOS), eq(evento.vagaId().toString()), anyString());
  }

  @Test
  void falhaPreservaPayloadParaRetomada() {
    var evento = publicador.registrar(UUID.randomUUID());
    String payload = repository.findById(evento.eventId()).orElseThrow().getPayload();
    when(kafka.send(anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Broker indisponivel")))
        .thenReturn(CompletableFuture.completedFuture(null));
    assertThatThrownBy(() -> publicador.publicar(evento.eventId())).isInstanceOf(IllegalStateException.class);
    assertThat(repository.findById(evento.eventId()).orElseThrow().getPublicadoEm()).isNull();
    publicador.publicar(evento.eventId());
    verify(kafka, times(2)).send(Topicos.VAGAS_RESULTADOS, evento.vagaId().toString(), payload);
  }

  @Test
  void intencaoParticipaDaTransacaoChamadora() {
    UUID vaga = UUID.randomUUID();
    assertThatThrownBy(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
      publicador.registrar(vaga);
      throw new IllegalStateException("Falha antes do commit");
    })).isInstanceOf(IllegalStateException.class);
    assertThat(repository.findAll()).noneMatch(e -> e.getVagaId().equals(vaga));
    verifyNoInteractions(kafka);
  }
}
