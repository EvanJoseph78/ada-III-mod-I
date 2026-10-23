package br.com.ada.selecaointerna.candidaturasservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CandidaturasPersistenceTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired CandidaturaRepository repository;

  @Test
  void gravaConsultaEImpedeInscricaoDuplicada() throws Exception {
    String payload = "{\"matriculaEmpregado\":123,\"idVaga\":1}";
    String body = mvc.perform(post("/api/v1/candidaturas").contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    UUID id = UUID.fromString(mapper.readTree(body).get("candidaturaId").asText());
    assertThat(repository.findById(id)).isPresent();
    mvc.perform(get("/api/v1/candidaturas/" + id)).andExpect(status().isOk())
        .andExpect(jsonPath("$.matriculaEmpregado").value(123))
        .andExpect(jsonPath("$.realizadaEm").isNotEmpty());
    mvc.perform(post("/api/v1/candidaturas").contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isConflict());
    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  void rejeitaEntradaInvalidaETrataConsultaInexistente() throws Exception {
    mvc.perform(post("/api/v1/candidaturas").contentType(MediaType.APPLICATION_JSON)
        .content("{\"idVaga\":-1}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/candidaturas/" + UUID.randomUUID())).andExpect(status().isNotFound());
  }
}
