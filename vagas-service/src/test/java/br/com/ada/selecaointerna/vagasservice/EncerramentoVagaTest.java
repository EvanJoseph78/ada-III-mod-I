package br.com.ada.selecaointerna.vagasservice;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class EncerramentoVagaTest {
  @Autowired VagasService vagas;
  @Autowired VagaRepository repository;
  @Autowired MockMvc mvc;
  @MockitoBean ProcessoCandidaturasClient candidaturas;

  @Test
  void encerraRepeteEImpedeExclusaoDoProcessoIniciado() throws Exception {
    var vaga = vagas.cadastrarVaga(new VagaInputDto("Java", List.of()));
    mvc.perform(post("/api/v1/vagas/" + vaga.vagaId() + "/encerramento"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_AVALIACAO"));
    vagas.encerrarInscricoes(vaga.vagaId());
    verify(candidaturas, times(2)).encerrar(vaga.vagaId(), vaga.idVaga());
    mvc.perform(delete("/api/v1/vagas/" + vaga.idVaga())).andExpect(status().isConflict());
    mvc.perform(post("/api/v1/vagas/" + UUID.randomUUID() + "/encerramento")).andExpect(status().isNotFound());
  }

  @Test
  void falhaRemotaNaoAvancaStatusERepeticaoPodeRecuperar() {
    var vaga = vagas.cadastrarVaga(new VagaInputDto("Java", List.of()));
    doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE)).doNothing()
        .when(candidaturas).encerrar(vaga.vagaId(), vaga.idVaga());
    assertThatThrownBy(() -> vagas.encerrarInscricoes(vaga.vagaId())).isInstanceOf(ResponseStatusException.class);
    assertThat(repository.findByVagaId(vaga.vagaId()).orElseThrow().getStatus().name()).isEqualTo("ABERTA");
    assertThat(vagas.encerrarInscricoes(vaga.vagaId()).status().name()).isEqualTo("EM_AVALIACAO");
  }
}
