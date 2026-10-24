package br.com.ada.selecaointerna.empregadosservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import br.com.ada.selecaointerna.empregadosservice.repository.jpa.EmpregadoRepositoryJPA;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
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
class EmpregadosPersistenceTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired EmpregadoRepositoryJPA repository;

  @BeforeEach
  void limparBanco() { repository.deleteAll(); }

  @Test
  void persistePerfilEmTransacaoSeparadaERejeitaMatriculaDuplicada() throws Exception {
    String payload = """
        {"matriculaEmpregado":123,"nomeEmpregado":"Ana",
         "itens":[{"tipo":"CURSO","descricao":"Java","horas":40,"meses":0}]}
        """;
    String body = mvc.perform(post("/api/v1/empregados").contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    UUID id = UUID.fromString(mapper.readTree(body).get("empregadoId").asText());
    assertThat(repository.findById(id)).isPresent();
    mvc.perform(get("/api/v1/empregados/" + id + "/perfil"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.empregadoId").value(id.toString()))
        .andExpect(jsonPath("$.itens[0].descricao").value("Java"));
    mvc.perform(get("/api/v1/empregados/123")).andExpect(status().isOk());
    mvc.perform(post("/api/v1/empregados").contentType(MediaType.APPLICATION_JSON).content(payload))
        .andExpect(status().isConflict());
    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  void validaCamposETrataEmpregadoInexistente() throws Exception {
    mvc.perform(post("/api/v1/empregados").contentType(MediaType.APPLICATION_JSON)
        .content("{\"matriculaEmpregado\":0,\"nomeEmpregado\":\"\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(post("/api/v1/empregados").contentType(MediaType.APPLICATION_JSON)
        .content("{\"matriculaEmpregado\":456,\"nomeEmpregado\":\"Bia\",\"itens\":[{\"tipo\":\"CURSO\",\"descricao\":\"Java\",\"horas\":-1}]}"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/empregados/999")).andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/empregados/" + UUID.randomUUID() + "/perfil"))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/empregados/999/curriculo-profissional")).andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/empregados/999/curriculo-educacional")).andExpect(status().isNotFound());
  }

  @Test
  void separaExperienciasDeFormacaoNasRotasDeCurriculo() throws Exception {
    mvc.perform(post("/api/v1/empregados").contentType(MediaType.APPLICATION_JSON).content("""
        {"matriculaEmpregado":456,"nomeEmpregado":"Bia","itens":[
          {"tipo":"CURSO","descricao":"Java","horas":40},
          {"tipo":"CERTIFICACAO","descricao":"Certificado Java"},
          {"tipo":"EXPERIENCIA_INTERNA","descricao":"Desenvolvimento","meses":12},
          {"tipo":"EXPERIENCIA_EXTERNA","descricao":"Consultoria","meses":6}]}
        """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.matriculaEmpregado").value(456))
        .andExpect(jsonPath("$.nomeEmpregado").value("Bia"));
    mvc.perform(get("/api/v1/empregados/456/curriculo-profissional"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(2))
        .andExpect(jsonPath("$.itens[0].tipo").value("EXPERIENCIA_INTERNA"))
        .andExpect(jsonPath("$.itens[1].tipo").value("EXPERIENCIA_EXTERNA"));
    mvc.perform(get("/api/v1/empregados/456/curriculo-educacional"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(2))
        .andExpect(jsonPath("$.itens[0].tipo").value("CURSO"))
        .andExpect(jsonPath("$.itens[1].tipo").value("CERTIFICACAO"));
    mvc.perform(get("/api/v1/empregados/456"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.itens.length()").value(4));
  }
}
