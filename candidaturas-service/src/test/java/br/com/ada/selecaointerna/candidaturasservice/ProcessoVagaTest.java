package br.com.ada.selecaointerna.candidaturasservice;

import br.com.ada.selecaointerna.contracts.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:processos;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @ActiveProfiles("test")
class ProcessoVagaTest {
  static final AtomicLong IDS = new AtomicLong(500);
  @Autowired ProcessoVagaService processos;
  @Autowired CandidaturasService candidaturas;
  @Autowired CandidaturaRepository repository;
  @Autowired AvaliacaoService avaliacao;
  @Autowired ObjectMapper mapper;
  @Autowired MockMvc mvc;
  @Autowired PlatformTransactionManager transactions;
  @MockitoBean DadosCandidaturaClient dados;
  UUID vaga;
  long id;
  DadosCandidaturaClient.Dados snapshot;

  @BeforeEach
  void preparar() {
    vaga = UUID.randomUUID(); id = IDS.incrementAndGet();
    snapshot = new DadosCandidaturaClient.Dados(new PerfilProfissionalDto(UUID.randomUUID(), "Ana", List.of()),
        new CriteriosVagaDto(vaga, "Java", StatusVaga.ABERTA, List.of()));
    when(dados.obter(any())).thenReturn(snapshot);
  }

  @Test
  void encerramentoERepeticaoPreservamProcessoVazioERejeitamIdsDivergentes() throws Exception {
    assertThat(processos.encerrar(vaga, id).apto()).isTrue();
    assertThat(processos.encerrar(vaga, id).inscricoes()).isZero();
    assertThatThrownBy(() -> processos.encerrar(vaga, id + 1)).isInstanceOf(ResponseStatusException.class);
    assertThatThrownBy(() -> processos.encerrar(UUID.randomUUID(), id)).isInstanceOf(ResponseStatusException.class);
    mvc.perform(get("/api/v1/candidaturas/processos/" + UUID.randomUUID() + "/prontidao")).andExpect(status().isNotFound());
    mvc.perform(post("/api/v1/candidaturas/processos/" + UUID.randomUUID() + "/encerramento")
        .contentType("application/json").content("{\"idVaga\":0}")).andExpect(status().isBadRequest());
  }

  @Test
  void inscricaoPendenteImpedeProntidaoAteAvaliacaoConfirmada() throws Exception {
    var c = candidaturas.realizarCandidatura(new CandidaturaInputDto(123L, id));
    assertThat(processos.encerrar(vaga, id).apto()).isFalse();
    var evento = mapper.readValue(repository.findById(c.candidaturaId()).orElseThrow().getEventoCandidatura(), CandidaturaRealizada.class);
    avaliacao.processar(evento);
    assertThat(processos.consultar(vaga).apto()).isTrue();
    avaliacao.processar(evento);
    assertThat(processos.consultar(vaga).avaliacoes()).isEqualTo(1);
  }

  @Test
  void consultaHttpIniciadaAntesDoFechamentoNaoPermiteInscricaoTardia() throws Exception {
    CountDownLatch consultando = new CountDownLatch(1), liberar = new CountDownLatch(1);
    when(dados.obter(any())).thenAnswer(invocation -> {
      consultando.countDown();
      if (!liberar.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout do teste");
      return snapshot;
    });
    try (var executor = Executors.newSingleThreadExecutor()) {
      Future<?> tentativa = executor.submit(() -> candidaturas.realizarCandidatura(new CandidaturaInputDto(123L, id)));
      try {
        assertThat(consultando.await(5, TimeUnit.SECONDS)).isTrue();
        processos.encerrar(vaga, id);
      } finally { liberar.countDown(); }
      assertThatThrownBy(() -> tentativa.get(5, TimeUnit.SECONDS)).isInstanceOf(ExecutionException.class)
          .hasCauseInstanceOf(ResponseStatusException.class);
      assertThat(repository.countByIdVaga(id)).isZero();
    }
  }

  @Test
  void fechamentoEsperaCommitDaInscricaoAdmitida() throws Exception {
    CountDownLatch gravada = new CountDownLatch(1), commit = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      Future<?> inscricao = executor.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
        candidaturas.realizarCandidatura(new CandidaturaInputDto(123L, id));
        gravada.countDown();
        try { if (!commit.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout do teste"); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
      }));
      assertThat(gravada.await(5, TimeUnit.SECONDS)).isTrue();
      Future<ProcessoVagaService.Prontidao> fechamento = executor.submit(() -> processos.encerrar(vaga, id));
      try { assertThatThrownBy(() -> fechamento.get(150, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class); }
      finally { commit.countDown(); }
      inscricao.get(5, TimeUnit.SECONDS);
      assertThat(fechamento.get(5, TimeUnit.SECONDS).inscricoes()).isEqualTo(1);
    }
  }
}
