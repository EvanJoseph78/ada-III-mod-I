package br.com.ada.selecaointerna.candidaturasservice;

import br.com.ada.selecaointerna.contracts.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:resultado;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @ActiveProfiles("test")
class ResultadoDefinitivoTest {
  static final AtomicLong IDS = new AtomicLong(1000);
  @Autowired ResultadoDefinitivoService resultados;
  @Autowired ProcessoVagaService processos;
  @Autowired CandidaturasService candidaturas;
  @Autowired CandidaturaRepository repository;
  @Autowired AvaliacaoService avaliador;
  @Autowired ObjectMapper mapper;
  @Autowired MockMvc mvc;
  @Autowired PlatformTransactionManager transactions;
  @MockitoBean DadosCandidaturaClient dados;

  @Test
  void publicaSnapshotPreservaEventoEDataEmReentregaConcorrente() throws Exception {
    var evento = preparar(true);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var a = executor.submit(() -> resultados.processar(evento));
      var b = executor.submit(() -> resultados.processar(evento));
      a.get(5, TimeUnit.SECONDS); b.get(5, TimeUnit.SECONDS);
    }
    var resultado = resultados.consultar(evento.vagaId());
    assertThat(resultado.publicacao()).isEqualTo(evento);
    assertThat(resultado.inscricoes()).isEqualTo(1);
    assertThat(resultado.ranking().classificados()).hasSize(1);
    resultados.processar(evento);
    assertThat(resultados.consultar(evento.vagaId())).isEqualTo(resultado);
    mvc.perform(get("/api/v1/candidaturas/resultados/" + evento.vagaId()))
        .andExpect(status().isOk()).andExpect(jsonPath("$.publicacao.eventId").value(evento.eventId().toString()));
    assertThatThrownBy(() -> resultados.processar(new ResultadoPublicado(UUID.randomUUID(), Instant.now(), evento.vagaId())))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> resultados.processar(new ResultadoPublicado(evento.eventId(), evento.ocorridoEm().plusSeconds(1), evento.vagaId())))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(resultados.consultar(evento.vagaId())).isEqualTo(resultado);
  }

  @Test
  void pendenciaImpedePublicacaoEAvaliacaoPermiteRetentarMesmoEvento() throws Exception {
    var evento = preparar(false);
    assertThatThrownBy(() -> resultados.processar(evento)).isInstanceOf(IllegalStateException.class);
    mvc.perform(get("/api/v1/candidaturas/resultados/" + evento.vagaId())).andExpect(status().isNotFound());
    var candidatura = repository.findAll().stream().filter(c -> {
      try { return mapper.readValue(c.getEventoCandidatura(), CandidaturaRealizada.class).vagaId().equals(evento.vagaId()); }
      catch (Exception e) { throw new IllegalStateException(e); }
    }).findFirst().orElseThrow();
    avaliador.processar(mapper.readValue(candidatura.getEventoCandidatura(), CandidaturaRealizada.class));
    resultados.processar(evento);
    assertThat(resultados.consultar(evento.vagaId()).avaliacoes()).isEqualTo(1);
  }

  @Test
  void resultadoVazioEValidoERollbackNaoMarcaPublicacao() {
    UUID vaga = UUID.randomUUID(); processos.encerrar(vaga, IDS.incrementAndGet());
    var evento = new ResultadoPublicado(UUID.randomUUID(), Instant.now(), vaga);
    assertThatThrownBy(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
      resultados.processar(evento); throw new IllegalStateException("Falha antes do commit");
    })).isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> resultados.consultar(vaga)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    resultados.processar(evento);
    assertThat(resultados.consultar(vaga).ranking().classificados()).isEmpty();
  }

  @Test
  void identificadoresAusentesOuProcessoNaoEncerradoSaoRejeitados() {
    assertThatThrownBy(() -> resultados.processar(null)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> resultados.processar(new ResultadoPublicado(null, Instant.now(), UUID.randomUUID())))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> resultados.processar(new ResultadoPublicado(UUID.randomUUID(), Instant.now(), UUID.randomUUID())))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private ResultadoPublicado preparar(boolean avaliar) throws Exception {
    UUID vaga = UUID.randomUUID(); long id = IDS.incrementAndGet();
    when(dados.obter(any())).thenReturn(new DadosCandidaturaClient.Dados(
        new PerfilProfissionalDto(UUID.randomUUID(), "Ana", List.of()),
        new CriteriosVagaDto(vaga, "Java", StatusVaga.ABERTA, List.of())));
    var c = candidaturas.realizarCandidatura(new CandidaturaInputDto(123L, id));
    if (avaliar) avaliador.processar(mapper.readValue(repository.findById(c.candidaturaId()).orElseThrow().getEventoCandidatura(), CandidaturaRealizada.class));
    processos.encerrar(vaga, id);
    return new ResultadoPublicado(UUID.randomUUID(), Instant.parse("2026-10-24T12:00:00.123456Z"), vaga);
  }
}
