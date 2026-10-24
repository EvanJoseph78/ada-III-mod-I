package br.com.ada.selecaointerna.candidaturasservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import br.com.ada.selecaointerna.contracts.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CandidaturasPersistenceTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired CandidaturaRepository repository;
  @MockitoBean DadosCandidaturaClient dadosClient;
  @Autowired OutboxRepository outbox;
  @Autowired AvaliacaoRepository avaliacoes;

  @BeforeEach
  void preparar() {
    avaliacoes.deleteAll();
    outbox.deleteAll();
    repository.deleteAll();
    when(dadosClient.obter(any())).thenReturn(new DadosCandidaturaClient.Dados(
        new PerfilProfissionalDto(UUID.randomUUID(), "Ana", List.of(new ItemPerfilDto(TipoItem.CURSO, "Java", 40, null))),
        new CriteriosVagaDto(UUID.randomUUID(), "Java", StatusVaga.ABERTA,
            List.of(new RequisitoDto(TipoItem.CURSO, "Java", true, 10, null)))));
  }

  @Test
  void gravaConsultaEImpedeInscricaoDuplicada() throws Exception {
    String payload = "{\"matriculaEmpregado\":123,\"idVaga\":1}";
    String body = mvc.perform(post("/api/v1/candidaturas").contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    UUID id = UUID.fromString(mapper.readTree(body).get("candidaturaId").asText());
    assertThat(repository.findById(id)).isPresent();
    CandidaturaRealizada evento = mapper.readValue(repository.findById(id).orElseThrow().getEventoCandidatura(), CandidaturaRealizada.class);
    assertThat(evento.candidaturaId()).isEqualTo(id);
    assertThat(evento.perfilSnapshot().itens().getFirst().descricao()).isEqualTo("Java");
    assertThat(evento.criteriosSnapshot().requisitos().getFirst().pontos()).isEqualTo(10);
    assertThat(outbox.findById(evento.eventId()).orElseThrow().getPayload()).isEqualTo(mapper.writeValueAsString(evento));
    mvc.perform(get("/api/v1/candidaturas/" + id)).andExpect(status().isOk())
        .andExpect(jsonPath("$.matriculaEmpregado").value(123))
        .andExpect(jsonPath("$.realizadaEm").isNotEmpty());
    mvc.perform(post("/api/v1/candidaturas").contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isConflict());
    assertThat(repository.count()).isEqualTo(1);
    verify(dadosClient, times(1)).obter(any());
  }

  @Test
  void naoGravaQuandoConsultaRemotaFalhaOuVagaEstaFechada() throws Exception {
    for (HttpStatus status : List.of(HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.CONFLICT, HttpStatus.UNPROCESSABLE_ENTITY)) {
      doThrow(new ResponseStatusException(status)).when(dadosClient).obter(any());
      mvc.perform(post("/api/v1/candidaturas").contentType(MediaType.APPLICATION_JSON)
          .content("{\"matriculaEmpregado\":123,\"idVaga\":1}"))
          .andExpect(status().is(status.value()));
      assertThat(repository.count()).isZero();
      assertThat(outbox.count()).isZero();
    }
  }

  @Test
  void rejeitaEntradaInvalidaETrataConsultaInexistente() throws Exception {
    mvc.perform(post("/api/v1/candidaturas").contentType(MediaType.APPLICATION_JSON)
        .content("{\"idVaga\":-1}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/candidaturas/" + UUID.randomUUID())).andExpect(status().isNotFound());
  }
}
