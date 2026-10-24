package br.com.ada.selecaointerna.candidaturasservice;

import br.com.ada.selecaointerna.contracts.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RankingConsultaTest {
  @Autowired CandidaturasService candidaturas;
  @Autowired CandidaturaRepository candidaturasRepository;
  @Autowired AvaliacaoRepository avaliacoes;
  @Autowired AvaliacaoService avaliacaoService;
  @Autowired OutboxRepository outbox;
  @Autowired RankingConsulta consulta;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;
  @Autowired MockMvc mvc;
  @MockitoBean DadosCandidaturaClient dadosClient;
  private final UUID vaga = UUID.randomUUID();

  @BeforeEach
  void limpar() { avaliacoes.deleteAll(); outbox.deleteAll(); candidaturasRepository.deleteAll(); }

  @Test
  void ordenaElegiveisPorPontosDataEIdExcluindoOutraVagaEInaptos() throws Exception {
    UUID segundo = registrar(vaga, 101L, 20, true, "2026-10-24T12:00:02Z");
    UUID primeiro = registrar(vaga, 102L, 20, true, "2026-10-24T12:00:01Z");
    UUID terceiro = registrar(vaga, 103L, 10, true, "2026-10-24T11:00:00Z");
    registrar(vaga, 104L, 100, false, "2026-10-24T10:00:00Z");
    registrar(UUID.randomUUID(), 105L, 500, true, "2026-10-24T10:00:00Z");
    var ranking = consulta.consultar(vaga);
    assertThat(ranking.classificados()).extracting(RankingDto.Classificado::candidaturaId)
        .containsExactly(primeiro, segundo, terceiro);
    assertThat(ranking.classificados()).extracting(RankingDto.Classificado::posicao).containsExactly(1, 2, 3);
    mvc.perform(get("/api/v1/candidaturas/ranking/" + vaga).accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk()).andExpect(jsonPath("$.vagaId").value(vaga.toString()))
        .andExpect(jsonPath("$.classificados[0].matriculaEmpregado").value(102))
        .andExpect(jsonPath("$.classificados[0].pontuacao").value(20));
  }

  @Test
  void rankingVazioNaoAfirmaExistenciaDaVagaEIdInvalidoRetorna400() throws Exception {
    mvc.perform(get("/api/v1/candidaturas/ranking/" + vaga))
        .andExpect(status().isOk()).andExpect(jsonPath("$.classificados").isEmpty());
    mvc.perform(get("/api/v1/candidaturas/ranking/nao-e-uuid")).andExpect(status().isBadRequest());
  }

  private UUID registrar(UUID vagaId, Long matricula, int pontos, boolean apto, String inscritaEm) throws Exception {
    when(dadosClient.obter(any())).thenReturn(new DadosCandidaturaClient.Dados(
        new PerfilProfissionalDto(UUID.randomUUID(), "Teste", apto
            ? List.of(new ItemPerfilDto(TipoItem.CURSO, "Java", 40, null)) : List.of()),
        new CriteriosVagaDto(vagaId, "Java", StatusVaga.ABERTA,
            List.of(new RequisitoDto(TipoItem.CURSO, "Java", true, pontos, null)))));
    var candidatura = candidaturas.realizarCandidatura(new CandidaturaInputDto(matricula, 1L));
    jdbc.update("update candidaturas set realizada_em=? where candidatura_id=?",
        Timestamp.from(Instant.parse(inscritaEm)), candidatura.candidaturaId());
    var evento = mapper.readValue(candidaturasRepository.findById(candidatura.candidaturaId()).orElseThrow()
        .getEventoCandidatura(), CandidaturaRealizada.class);
    avaliacaoService.processar(evento);
    return candidatura.candidaturaId();
  }
}
