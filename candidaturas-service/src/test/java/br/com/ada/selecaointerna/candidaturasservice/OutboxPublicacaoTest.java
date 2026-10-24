package br.com.ada.selecaointerna.candidaturasservice;

import br.com.ada.selecaointerna.contracts.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class OutboxPublicacaoTest {
  @Autowired CandidaturasService candidaturas;
  @Autowired CandidaturaRepository repository;
  @Autowired PublicacaoOutboxService publicacao;
  @MockitoSpyBean OutboxRepository outbox;
  @MockitoBean DadosCandidaturaClient dadosClient;
  @MockitoBean KafkaTemplate<String, String> kafka;

  @BeforeEach
  void preparar() {
    outbox.deleteAll();
    repository.deleteAll();
    when(dadosClient.obter(any())).thenReturn(new DadosCandidaturaClient.Dados(
        new PerfilProfissionalDto(UUID.randomUUID(), "Ana", List.of()),
        new CriteriosVagaDto(UUID.randomUUID(), "Java", StatusVaga.ABERTA, List.of())));
    when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
  }

  @Test
  void confirmaPublicacaoSomenteAposAckENaoReenviaRegistroConfirmado() {
    candidaturas.realizarCandidatura(new CandidaturaInputDto(123L, 1L));
    OutboxCandidatura evento = outbox.findAll().getFirst();
    assertThat(evento.getPublicadoEm()).isNull();
    publicacao.publicar(evento.getEventId());
    assertThat(outbox.findById(evento.getEventId()).orElseThrow().getPublicadoEm()).isNotNull();
    publicacao.publicar(evento.getEventId());
    verify(kafka, times(1)).send(Topicos.CANDIDATURAS_REALIZADAS, evento.getCandidaturaId().toString(), evento.getPayload());
  }

  @Test
  void falhaDeEnvioPreservaPendenciaParaNovaTentativaComMesmoPayload() {
    candidaturas.realizarCandidatura(new CandidaturaInputDto(123L, 1L));
    OutboxCandidatura evento = outbox.findAll().getFirst();
    when(kafka.send(anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Broker indisponivel")));
    assertThatThrownBy(() -> publicacao.publicar(evento.getEventId())).isInstanceOf(IllegalStateException.class);
    assertThat(outbox.findById(evento.getEventId()).orElseThrow().getPublicadoEm()).isNull();
    when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
    publicacao.publicar(evento.getEventId());
    assertThat(outbox.findById(evento.getEventId()).orElseThrow().getPublicadoEm()).isNotNull();
    verify(kafka, times(2)).send(Topicos.CANDIDATURAS_REALIZADAS, evento.getCandidaturaId().toString(), evento.getPayload());
  }

  @Test
  void falhaAoGravarOutboxDesfazTambemACandidatura() {
    doThrow(new DataIntegrityViolationException("Falha de escrita simulada"))
        .when(outbox).saveAndFlush(any(OutboxCandidatura.class));
    assertThatThrownBy(() -> candidaturas.realizarCandidatura(new CandidaturaInputDto(123L, 1L)))
        .isInstanceOf(ResponseStatusException.class);
    assertThat(repository.count()).isZero();
    assertThat(outbox.count()).isZero();
  }
}
