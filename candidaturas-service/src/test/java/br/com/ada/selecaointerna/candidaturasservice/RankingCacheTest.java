package br.com.ada.selecaointerna.candidaturasservice;

import br.com.ada.selecaointerna.contracts.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ActiveProfiles("test")
class RankingCacheTest {
  @Autowired RankingService ranking;
  @Autowired CandidaturasService candidaturas;
  @Autowired CandidaturaRepository candidaturasRepository;
  @Autowired AvaliacaoService avaliacaoService;
  @Autowired AvaliacaoRepository avaliacoes;
  @Autowired OutboxRepository outbox;
  @Autowired CacheManager cacheManager;
  @Autowired ObjectMapper mapper;
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
}
