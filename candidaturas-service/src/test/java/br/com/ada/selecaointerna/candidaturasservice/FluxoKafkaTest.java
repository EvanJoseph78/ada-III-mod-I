package br.com.ada.selecaointerna.candidaturasservice;

import br.com.ada.selecaointerna.contracts.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.*;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(properties = {
    "mensageria.enabled=true", "mensageria.outbox.intervalo-ms=3600000", "mensageria.retry.intervalo-ms=50",
    "spring.datasource.url=jdbc:h2:mem:fluxo-kafka;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"})
@ActiveProfiles("test")
@EmbeddedKafka(kraft = true, partitions = 3,
    topics = {Topicos.CANDIDATURAS_REALIZADAS, Topicos.CANDIDATURAS_REALIZADAS + ".DLT",
        Topicos.VAGAS_RESULTADOS, Topicos.VAGAS_RESULTADOS + ".DLT"},
    bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@DirtiesContext
class FluxoKafkaTest {
  private static final String DLT = Topicos.CANDIDATURAS_REALIZADAS + ".DLT";
  @Autowired CandidaturasService candidaturas;
  @Autowired CandidaturaRepository repository;
  @Autowired AvaliacaoRepository avaliacoes;
  @Autowired OutboxRepository outbox;
  @Autowired OutboxDispatcher dispatcher;
  @Autowired ObjectMapper mapper;
  @Autowired KafkaTemplate<String, String> kafka;
  @Autowired EmbeddedKafkaBroker broker;
  @Autowired KafkaListenerEndpointRegistry registry;
  @MockitoBean DadosCandidaturaClient dadosClient;
  @MockitoSpyBean AvaliadorCurricular avaliador;
  @MockitoSpyBean CandidaturaKafkaListener listener;
  @Autowired ProcessoVagaService processos;
  @MockitoSpyBean ResultadoDefinitivoService resultados;
  @MockitoSpyBean ResultadoKafkaListener resultadoListener;

  @BeforeEach
  void preparar() {
    ContainerTestUtils.waitForAssignment(registry.getListenerContainer("avaliacao-curricular"), 3);
    ContainerTestUtils.waitForAssignment(registry.getListenerContainer("resultado-publicado"), 3);
    avaliacoes.deleteAll(); outbox.deleteAll(); repository.deleteAll();
    when(dadosClient.obter(any())).thenReturn(new DadosCandidaturaClient.Dados(
        new PerfilProfissionalDto(UUID.randomUUID(), "Ana", List.of(new ItemPerfilDto(TipoItem.CURSO, "Java", 40, null))),
        new CriteriosVagaDto(UUID.randomUUID(), "Java", StatusVaga.ABERTA,
            List.of(new RequisitoDto(TipoItem.CURSO, "Java", true, 10, null)))));
  }

  @Test
  void outboxPublicaConsomeEReentregaNaoDuplicaResultado() throws Exception {
    var candidatura = candidaturas.realizarCandidatura(new CandidaturaInputDto(123L, 1L));
    String payload = repository.findById(candidatura.candidaturaId()).orElseThrow().getEventoCandidatura();
    AtomicInteger concluidos = new AtomicInteger();
    doAnswer(invocation -> {
      Object resultado = invocation.callRealMethod();
      concluidos.incrementAndGet();
      return resultado;
    }).when(listener).receber(eq(payload));
    assertThat(candidatura.status()).isEqualTo("PENDENTE");
    dispatcher.publicarPendentes();
    await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(concluidos.get()).isEqualTo(1));
    assertThat(outbox.findAll().getFirst().getPublicadoEm()).isNotNull();
    var primeira = avaliacoes.findById(candidatura.candidaturaId()).orElseThrow();
    kafka.send(Topicos.CANDIDATURAS_REALIZADAS, candidatura.candidaturaId().toString(), payload).get(10, TimeUnit.SECONDS);
    await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(concluidos.get()).isEqualTo(2));
    assertThat(avaliacoes.count()).isEqualTo(1);
    assertThat(avaliacoes.findById(candidatura.candidaturaId()).orElseThrow().getAvaliadaEm()).isEqualTo(primeira.getAvaliadaEm());
    assertThat(candidaturas.obterCandidatura(candidatura.candidaturaId()).pontuacao()).isEqualTo(10);
    verify(avaliador, times(1)).avaliar(any(), any());
  }

  @Test
  void falhaTransitoriaRecebeRetryEConcluiSemDuplicar() {
    doThrow(new TransientDataAccessResourceException("Falha transitoria simulada"))
        .doCallRealMethod().when(avaliador).avaliar(any(), any());
    var candidatura = candidaturas.realizarCandidatura(new CandidaturaInputDto(123L, 1L));
    dispatcher.publicarPendentes();
    await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
        assertThat(candidaturas.obterCandidatura(candidatura.candidaturaId()).status()).isEqualTo("AVALIADA"));
    assertThat(avaliacoes.count()).isEqualTo(1);
    verify(avaliador, times(2)).avaliar(any(), any());
  }

  @Test
  void jsonInvalidoVaiParaDltSemTentarCalcular() throws Exception {
    try (Consumer<String, String> consumer = consumidorDlt()) {
      String invalido = "json-invalido-" + UUID.randomUUID();
      kafka.send(Topicos.CANDIDATURAS_REALIZADAS, UUID.randomUUID().toString(), invalido).get(10, TimeUnit.SECONDS);
      esperarDlt(consumer, invalido);
      assertThat(avaliacoes.count()).isZero();
      verifyNoInteractions(avaliador);
    }
  }

  @Test
  void falhaQuePersisteEsgotaTresTentativasEVaiParaDltSemGravarAvaliacao() {
    doThrow(new TransientDataAccessResourceException("Banco temporariamente indisponivel"))
        .when(avaliador).avaliar(any(), any());
    var candidatura = candidaturas.realizarCandidatura(new CandidaturaInputDto(123L, 1L));
    String payload = repository.findById(candidatura.candidaturaId()).orElseThrow().getEventoCandidatura();
    try (Consumer<String, String> consumer = consumidorDlt()) {
      dispatcher.publicarPendentes();
      esperarDlt(consumer, payload);
      assertThat(avaliacoes.count()).isZero();
      assertThat(candidaturas.obterCandidatura(candidatura.candidaturaId()).status()).isEqualTo("PENDENTE");
      verify(avaliador, times(3)).avaliar(any(), any());
    }
  }

  private Consumer<String, String> consumidorDlt() {
    return consumidorDlt(DLT);
  }

  @Test
  void resultadoConsomeReentregaPreservaSnapshotEDivergenciaVaiParaDlt() throws Exception {
    var evento = resultadoPronto();
    String payload = mapper.writeValueAsString(evento);
    kafka.send(Topicos.VAGAS_RESULTADOS, evento.vagaId().toString(), payload).get(10, TimeUnit.SECONDS);
    await().atMost(Duration.ofSeconds(15)).ignoreException(org.springframework.web.server.ResponseStatusException.class)
        .untilAsserted(() -> assertThat(resultados.consultar(evento.vagaId()).publicacao()).isEqualTo(evento));
    var original = resultados.consultar(evento.vagaId());
    kafka.send(Topicos.VAGAS_RESULTADOS, evento.vagaId().toString(), payload).get(10, TimeUnit.SECONDS);
    await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> verify(resultadoListener, times(2)).receber(payload));
    assertThat(resultados.consultar(evento.vagaId())).isEqualTo(original);
    try (var consumer = consumidorDlt(Topicos.VAGAS_RESULTADOS + ".DLT")) {
      String divergente = mapper.writeValueAsString(new ResultadoPublicado(UUID.randomUUID(), evento.ocorridoEm(), evento.vagaId()));
      kafka.send(Topicos.VAGAS_RESULTADOS, evento.vagaId().toString(), divergente).get(10, TimeUnit.SECONDS);
      esperarDlt(consumer, divergente);
      assertThat(resultados.consultar(evento.vagaId())).isEqualTo(original);
    }
  }

  @Test
  void resultadoComFalhaTransitoriaRetentaEJsonInvalidoVaiParaDlt() throws Exception {
    var evento = resultadoPronto();
    doThrow(new TransientDataAccessResourceException("Falha transitoria simulada")).doCallRealMethod()
        .when(resultados).processar(evento);
    kafka.send(Topicos.VAGAS_RESULTADOS, evento.vagaId().toString(), mapper.writeValueAsString(evento)).get(10, TimeUnit.SECONDS);
    await().atMost(Duration.ofSeconds(15)).ignoreException(org.springframework.web.server.ResponseStatusException.class)
        .untilAsserted(() -> assertThat(resultados.consultar(evento.vagaId()).publicacao()).isEqualTo(evento));
    verify(resultados, times(2)).processar(evento);
    try (var consumer = consumidorDlt(Topicos.VAGAS_RESULTADOS + ".DLT")) {
      String invalido = "resultado-invalido-" + UUID.randomUUID();
      kafka.send(Topicos.VAGAS_RESULTADOS, UUID.randomUUID().toString(), invalido).get(10, TimeUnit.SECONDS);
      esperarDlt(consumer, invalido);
    }
  }

  private ResultadoPublicado resultadoPronto() throws Exception {
    // IDs distintos evitam reutilizar a barreira persistente entre testes.
    long idVaga = Math.abs(UUID.randomUUID().getMostSignificantBits() / 2) + 100;
    var c = candidaturas.realizarCandidatura(new CandidaturaInputDto(123L, idVaga));
    dispatcher.publicarPendentes();
    await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(candidaturas.obterCandidatura(c.candidaturaId()).status()).isEqualTo("AVALIADA"));
    var candidatura = mapper.readValue(repository.findById(c.candidaturaId()).orElseThrow().getEventoCandidatura(), CandidaturaRealizada.class);
    processos.encerrar(candidatura.vagaId(), idVaga);
    return new ResultadoPublicado(UUID.randomUUID(), java.time.Instant.now(), candidatura.vagaId());
  }

  private Consumer<String, String> consumidorDlt(String topico) {
    var props = KafkaTestUtils.consumerProps("verificacao-dlt-" + UUID.randomUUID(), "false", broker);
    Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props,
        new StringDeserializer(), new StringDeserializer()).createConsumer();
    broker.consumeFromAnEmbeddedTopic(consumer, topico);
    return consumer;
  }

  private void esperarDlt(Consumer<String, String> consumer, String esperado) {
    long prazo = System.nanoTime() + Duration.ofSeconds(15).toNanos();
    while (System.nanoTime() < prazo) {
      for (var record : consumer.poll(Duration.ofMillis(200))) {
        if (esperado.equals(record.value())) return;
      }
    }
    fail("Mensagem esperada nao chegou a DLT");
  }
}
