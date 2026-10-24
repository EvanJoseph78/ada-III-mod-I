package br.com.ada.selecaointerna.vagasservice;

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
class VagasPersistenceTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired VagaRepository repository;

  @Test
  void persisteCriteriosEExcluiPorIdSemReutilizarIdentificador() throws Exception {
    String body = cadastrar("Java");
    long id = mapper.readTree(body).get("idVaga").asLong();
    String uuid = mapper.readTree(body).get("vagaId").asText();
    assertThat(repository.findById(id)).isPresent();
    mvc.perform(get("/api/v1/vagas/" + uuid + "/criterios"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ABERTA"))
        .andExpect(jsonPath("$.requisitos[0].pontos").value(10));
    long outroId = mapper.readTree(cadastrar("Outra")).get("idVaga").asLong();
    mvc.perform(delete("/api/v1/vagas/" + id)).andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/vagas/" + id)).andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/vagas/" + outroId)).andExpect(status().isOk());
    long novoId = mapper.readTree(cadastrar("Nova")).get("idVaga").asLong();
    assertThat(novoId).isGreaterThan(outroId);
  }

  @Test
  void validaCriteriosETrataVagaInexistente() throws Exception {
    mvc.perform(post("/api/v1/vagas").contentType(MediaType.APPLICATION_JSON)
        .content("{\"nomeVaga\":\"Java\",\"requisitos\":[{\"tipo\":\"CURSO\",\"palavraChave\":\"Java\",\"pontos\":-1}]}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/vagas/999999")).andExpect(status().isNotFound());
    mvc.perform(delete("/api/v1/vagas/999999")).andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/vagas/" + UUID.randomUUID() + "/criterios"))
        .andExpect(status().isNotFound());
  }

  private String cadastrar(String nome) throws Exception {
    return mvc.perform(post("/api/v1/vagas").contentType(MediaType.APPLICATION_JSON).content("""
        {"nomeVaga":"%s","requisitos":[{"tipo":"CURSO","palavraChave":"Java","obrigatorio":true,"pontos":10}]}
        """.formatted(nome))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
  }
}
