package br.com.ada.selecaointerna.candidaturasservice;

import br.com.ada.selecaointerna.contracts.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.Cache;
import org.springframework.context.annotation.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest
@ActiveProfiles("test")
@Import(RankingCacheTest.RelogioConfig.class)
class RankingCacheTest {
  @Autowired RankingService ranking;
  @Autowired CandidaturasService candidaturas;
  @Autowired CandidaturaRepository candidaturasRepository;
  @Autowired AvaliacaoService avaliacaoService;
  @Autowired AvaliacaoRepository avaliacoes;
  @Autowired OutboxRepository outbox;
  @MockitoSpyBean CacheManager cacheManager;
  @Autowired ObjectMapper mapper;
  @Autowired RelogioCache relogio;
  @Autowired PlatformTransactionManager transactionManager;
  @MockitoSpyBean RankingConsulta consulta;
  @MockitoBean DadosCandidaturaClient dadosClient;
  private final UUID vaga = UUID.randomUUID();

  @BeforeEach
  void preparar() {
    cacheManager.getCache(RankingCacheConfig.CACHE).clear();
    avaliacoes.deleteAll(); outbox.deleteAll(); candidaturasRepository.deleteAll();
  }

  @Test
  void preencheCacheEReutilizaSemRepetirConsultaCompleta() throws Exception {
    registrar(vaga, 123L, 10, true);
    var primeira = ranking.obter(vaga);
    var segunda = ranking.obter(vaga);
    assertThat(segunda).isEqualTo(primeira);
    assertThat(segunda.classificados()).hasSize(1);
    verify(consulta, times(1)).consultar(vaga);
  }

  @Test
  void invalidaLogicamenteSomenteDepoisDoCommitDaNovaAvaliacao() throws Exception {
    registrar(vaga, 123L, 10, true);
    assertThat(ranking.obter(vaga).classificados()).hasSize(1);
    var pendente = registrar(vaga, 124L, 20, false);
    assertThat(ranking.obter(vaga).classificados()).hasSize(1);
    verify(consulta, times(1)).consultar(vaga);
    avaliacaoService.processar(pendente);
    var atualizado = ranking.obter(vaga);
    assertThat(atualizado.classificados()).hasSize(2);
    assertThat(atualizado.classificados().getFirst().pontuacao()).isEqualTo(20);
    verify(consulta, times(2)).consultar(vaga);
    assertThat(cacheManager.getCache(RankingCacheConfig.CACHE).get(vaga + ":1")).isNotNull();
    assertThat(cacheManager.getCache(RankingCacheConfig.CACHE).get(vaga + ":2")).isNotNull();
  }

  @Test
  void avaliacaoDeOutraVagaNaoInvalidaRankingDaPrimeira() throws Exception {
    registrar(vaga, 123L, 10, true);
    ranking.obter(vaga);
    registrar(UUID.randomUUID(), 124L, 100, true);
    assertThat(ranking.obter(vaga).classificados()).hasSize(1);
    verify(consulta, times(1)).consultar(vaga);
  }

  @Test
  void reentregaIdempotenteNaoTrocaRevisaoNemInvalidaCache() throws Exception {
    var evento = registrar(vaga, 123L, 10, true);
    var primeira = ranking.obter(vaga);
    avaliacaoService.processar(evento);
    assertThat(ranking.obter(vaga)).isEqualTo(primeira);
    verify(consulta, times(1)).consultar(vaga);
    assertThat(avaliacoes.countByVagaId(vaga)).isEqualTo(1);
  }

  @Test
  void ttlExpiraDesdeAEscritaMesmoComAcessoIntermediario() throws Exception {
    registrar(vaga, 123L, 10, true);
    var primeiro = ranking.obter(vaga);
    relogio.avancar(Duration.ofSeconds(59));
    assertThat(ranking.obter(vaga)).isEqualTo(primeiro);
    verify(consulta, times(1)).consultar(vaga);
    relogio.avancar(Duration.ofSeconds(2));
    assertThat(ranking.obter(vaga)).isEqualTo(primeiro);
    verify(consulta, times(2)).consultar(vaga);
  }

  @Test
  void falhaDeLeituraUsaBancoESemEscritaNoCacheMasNaoEscondeFalhaDaFonte() throws Exception {
    registrar(vaga, 123L, 10, true);
    Cache indisponivel = mock(Cache.class);
    when(indisponivel.get(any(), eq(RankingDto.class))).thenThrow(new IllegalStateException("Cache indisponivel"));
    doReturn(indisponivel).when(cacheManager).getCache(RankingCacheConfig.CACHE);
    assertThat(ranking.obter(vaga).classificados()).hasSize(1);
    verify(indisponivel, never()).put(any(), any());
    doThrow(new DataAccessResourceFailureException("Fonte indisponivel")).when(consulta).consultar(vaga);
    assertThatThrownBy(() -> ranking.obter(vaga)).isInstanceOf(DataAccessResourceFailureException.class);
  }

  @Test
  void falhaDeEscritaNoCachePreservaRespostaConsultadaNoBanco() throws Exception {
    registrar(vaga, 123L, 10, true);
    Cache indisponivel = mock(Cache.class);
    doThrow(new IllegalStateException("Falha de escrita")).when(indisponivel).put(any(), any());
    doReturn(indisponivel).when(cacheManager).getCache(RankingCacheConfig.CACHE);
    assertThat(ranking.obter(vaga).classificados()).hasSize(1);
    verify(consulta, times(1)).consultar(vaga);
    verify(indisponivel).put(any(), any());
  }

  @Test
  void rollbackAposGravarAvaliacaoNaoInvalidaRankingAnterior() throws Exception {
    registrar(vaga, 123L, 10, true);
    var primeiro = ranking.obter(vaga);
    var pendente = registrar(vaga, 124L, 20, false);
    var transaction = new TransactionTemplate(transactionManager);
    assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
      avaliacaoService.processar(pendente);
      throw new IllegalStateException("Falha depois da escrita, antes do commit");
    })).isInstanceOf(IllegalStateException.class);
    assertThat(avaliacoes.countByVagaId(vaga)).isEqualTo(1);
    assertThat(ranking.obter(vaga)).isEqualTo(primeiro);
    verify(consulta, times(1)).consultar(vaga);
    avaliacaoService.processar(pendente);
    assertThat(ranking.obter(vaga).classificados()).hasSize(2);
    verify(consulta, times(2)).consultar(vaga);
  }

  @Test
  void preenchimentoAntigoAtrasadoNaoSobrescreveRankingDaRevisaoNova() throws Exception {
    registrar(vaga, 123L, 10, true);
    CountDownLatch lido = new CountDownLatch(1);
    CountDownLatch liberar = new CountDownLatch(1);
    AtomicBoolean primeira = new AtomicBoolean(true);
    doAnswer(invocation -> {
      var resultado = invocation.callRealMethod();
      if (primeira.getAndSet(false)) {
        lido.countDown();
        if (!liberar.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Timeout no teste");
      }
      return resultado;
    }).when(consulta).consultar(vaga);
    try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
      Future<RankingDto> antiga = executor.submit(() -> ranking.obter(vaga));
      try {
        assertThat(lido.await(10, TimeUnit.SECONDS)).isTrue();
        registrar(vaga, 124L, 20, true);
        assertThat(ranking.obter(vaga).classificados()).hasSize(2);
      } finally { liberar.countDown(); }
      assertThat(antiga.get(10, TimeUnit.SECONDS).classificados()).hasSize(1);
      assertThat(ranking.obter(vaga).classificados()).hasSize(2);
      verify(consulta, times(2)).consultar(vaga);
    }
  }

  private CandidaturaRealizada registrar(UUID vagaId, Long matricula, int pontos, boolean avaliar) throws Exception {
    when(dadosClient.obter(any())).thenReturn(new DadosCandidaturaClient.Dados(
        new PerfilProfissionalDto(UUID.randomUUID(), "Teste", List.of(new ItemPerfilDto(TipoItem.CURSO, "Java", 40, null))),
        new CriteriosVagaDto(vagaId, "Java", StatusVaga.ABERTA,
            List.of(new RequisitoDto(TipoItem.CURSO, "Java", true, pontos, null)))));
    var candidatura = candidaturas.realizarCandidatura(new CandidaturaInputDto(matricula, 1L));
    var evento = mapper.readValue(candidaturasRepository.findById(candidatura.candidaturaId()).orElseThrow()
        .getEventoCandidatura(), CandidaturaRealizada.class);
    if (avaliar) avaliacaoService.processar(evento);
    return evento;
  }

  @TestConfiguration
  static class RelogioConfig {
    @Bean RelogioCache relogioCache() { return new RelogioCache(); }
  }

  static class RelogioCache implements Ticker {
    private final AtomicLong nanos = new AtomicLong();
    @Override public long read() { return nanos.get(); }
    void avancar(Duration duration) { nanos.addAndGet(duration.toNanos()); }
  }
}
