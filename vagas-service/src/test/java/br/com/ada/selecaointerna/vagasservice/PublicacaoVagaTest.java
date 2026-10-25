package br.com.ada.selecaointerna.vagasservice;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class PublicacaoVagaTest {
  @Autowired VagasService vagas;
  @Autowired VagaRepository repository;
  @Autowired MockMvc mvc;
  @MockitoSpyBean ResultadoOutboxRepository outbox;
  @MockitoBean ProcessoCandidaturasClient candidaturas;

  @Test
  void exigeFechamentoPublicaUmaVezEConcluiProcesso() throws Exception {
    var vaga = vagas.cadastrarVaga(new VagaInputDto("Java", List.of()));
    mvc.perform(post("/api/v1/vagas/" + vaga.vagaId() + "/resultado")).andExpect(status().isConflict());
    mvc.perform(post("/api/v1/vagas/" + vaga.vagaId() + "/conclusao")).andExpect(status().isConflict());
    vagas.encerrarInscricoes(vaga.vagaId());
    mvc.perform(post("/api/v1/vagas/" + vaga.vagaId() + "/resultado"))
        .andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("RESULTADO_PUBLICADO"));
    mvc.perform(post("/api/v1/vagas/" + vaga.vagaId() + "/resultado")).andExpect(status().isConflict());
    mvc.perform(post("/api/v1/vagas/" + vaga.vagaId() + "/encerramento")).andExpect(status().isConflict());
    assertThat(outbox.findAll().stream().filter(e -> e.getVagaId().equals(vaga.vagaId())).count()).isEqualTo(1);
    assertThat(vagas.concluir(vaga.vagaId()).status().name()).isEqualTo("CONCLUIDA");
  }

  @Test
  void avaliacaoPendenteNaoMudaStatusNemCriaOutbox() {
    var vaga = fechada();
    doThrow(new ResponseStatusException(HttpStatus.CONFLICT)).when(candidaturas).exigirProntidao(vaga.vagaId(), vaga.idVaga());
    assertThatThrownBy(() -> vagas.publicarResultado(vaga.vagaId())).isInstanceOf(ResponseStatusException.class);
    verificarSemPublicacao(vaga.vagaId());
  }

  @Test
  void erroNaOutboxReverteTambemTransicaoDaVaga() {
    var vaga = fechada();
    doThrow(new DataIntegrityViolationException("Falha simulada")).when(outbox).saveAndFlush(any());
    assertThatThrownBy(() -> vagas.publicarResultado(vaga.vagaId())).isInstanceOf(DataIntegrityViolationException.class);
    verificarSemPublicacao(vaga.vagaId());
  }

  @Test
  void comandosConcorrentesCriamApenasUmaPublicacao() throws Exception {
    var vaga = fechada();
    try (var executor = Executors.newFixedThreadPool(2)) {
      var inicio = new CountDownLatch(1);
      Callable<Integer> publicar = () -> {
        inicio.await(5, TimeUnit.SECONDS);
        try { vagas.publicarResultado(vaga.vagaId()); return 202; }
        catch (ResponseStatusException e) { return e.getStatusCode().value(); }
      };
      var a = executor.submit(publicar); var b = executor.submit(publicar); inicio.countDown();
      assertThat(List.of(a.get(5, TimeUnit.SECONDS), b.get(5, TimeUnit.SECONDS))).containsExactlyInAnyOrder(202, 409);
      assertThat(outbox.findAll().stream().filter(e -> e.getVagaId().equals(vaga.vagaId())).count()).isEqualTo(1);
    }
  }

  private VagaOutputDto fechada() {
    var vaga = vagas.cadastrarVaga(new VagaInputDto("Java", List.of()));
    vagas.encerrarInscricoes(vaga.vagaId()); return vaga;
  }
  private void verificarSemPublicacao(UUID vaga) {
    assertThat(repository.findByVagaId(vaga).orElseThrow().getStatus().name()).isEqualTo("EM_AVALIACAO");
    assertThat(outbox.findAll()).noneMatch(e -> e.getVagaId().equals(vaga));
  }
}
