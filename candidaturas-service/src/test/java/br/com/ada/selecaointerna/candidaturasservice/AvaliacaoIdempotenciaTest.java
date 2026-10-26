package br.com.ada.selecaointerna.candidaturasservice;

import br.com.ada.selecaointerna.contracts.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ActiveProfiles("test")
class AvaliacaoIdempotenciaTest {
  @Autowired CandidaturasService candidaturas;
  @Autowired CandidaturaRepository repository;
  @Autowired AvaliacaoRepository avaliacoes;
  @Autowired OutboxRepository outbox;
  @Autowired AvaliacaoService service;
  @Autowired ObjectMapper mapper;
  @MockitoBean DadosCandidaturaClient dadosClient;
  @MockitoSpyBean AvaliadorCurricular avaliador;

  @BeforeEach
  void preparar() {
    avaliacoes.deleteAll(); outbox.deleteAll(); repository.deleteAll();
    when(dadosClient.obter(any())).thenReturn(new DadosCandidaturaClient.Dados(
        new PerfilProfissionalDto(UUID.randomUUID(), "Ana", List.of(new ItemPerfilDto(TipoItem.CURSO, "Java", 40, null))),
        new CriteriosVagaDto(UUID.randomUUID(), "Java", StatusVaga.ABERTA,
            List.of(new RequisitoDto(TipoItem.CURSO, "Java", true, 10, null)))));
  }

  @Test
  void reentregaPreservaUmaUnicaAvaliacaoEPontuacaoSemSomarNovamente() throws Exception {
    CandidaturaRealizada evento = registrar();
    assertThat(candidaturas.obterCandidatura(evento.candidaturaId()).status()).isEqualTo("PENDENTE");
    service.processar(evento);
    var instante = avaliacoes.findById(evento.candidaturaId()).orElseThrow().getAvaliadaEm();
    service.processar(evento);
    assertThat(avaliacoes.count()).isEqualTo(1);
    assertThat(avaliacoes.findById(evento.candidaturaId()).orElseThrow().getAvaliadaEm()).isEqualTo(instante);
    var resultado = candidaturas.obterCandidatura(evento.candidaturaId());
    assertThat(resultado.status()).isEqualTo("AVALIADA");
    assertThat(resultado.pontuacao()).isEqualTo(10);
    assertThat(resultado.elegivel()).isTrue();
    verify(avaliador, times(1)).avaliar(any(), any());
  }

  @Test
  void entregasConcorrentesTambemPersistemUmaUnicaAvaliacao() throws Exception {
    CandidaturaRealizada evento = registrar();
    CountDownLatch inicio = new CountDownLatch(1);
    try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
      Callable<Void> tarefa = () -> { inicio.await(); service.processar(evento); return null; };
      Future<Void> primeira = executor.submit(tarefa);
      Future<Void> segunda = executor.submit(tarefa);
      inicio.countDown();
      primeira.get(10, TimeUnit.SECONDS); segunda.get(10, TimeUnit.SECONDS);
    }
    assertThat(avaliacoes.count()).isEqualTo(1);
    verify(avaliador, times(1)).avaliar(any(), any());
  }

  @Test
  void eventoAdulteradoNaoAlteraAvaliacaoNemSnapshot() throws Exception {
    CandidaturaRealizada evento = registrar();
    var alterado = new CandidaturaRealizada(UUID.randomUUID(), evento.ocorridoEm(), evento.candidaturaId(),
        evento.vagaId(), evento.empregadoId(), evento.perfilSnapshot(), evento.criteriosSnapshot());
    assertThatThrownBy(() -> service.processar(alterado)).isInstanceOf(IllegalArgumentException.class);
    assertThat(avaliacoes.count()).isZero();
    service.processar(evento);
    assertThat(avaliacoes.count()).isEqualTo(1);
  }

  @Test
  void falhaTransitoriaNaoMarcaComoProcessadoENovaTentativaConclui() throws Exception {
    CandidaturaRealizada evento = registrar();
    doThrow(new TransientDataAccessResourceException("Falha transitoria simulada"))
        .when(avaliador).avaliar(any(), any());
    assertThatThrownBy(() -> service.processar(evento)).isInstanceOf(TransientDataAccessResourceException.class);
    assertThat(avaliacoes.count()).isZero();
    doCallRealMethod().when(avaliador).avaliar(any(), any());
    service.processar(evento);
    assertThat(avaliacoes.count()).isEqualTo(1);
  }

  private CandidaturaRealizada registrar() throws Exception {
    var candidatura = candidaturas.realizarCandidatura(new CandidaturaInputDto(123L, 1L));
    return mapper.readValue(repository.findById(candidatura.candidaturaId()).orElseThrow().getEventoCandidatura(), CandidaturaRealizada.class);
  }
}
