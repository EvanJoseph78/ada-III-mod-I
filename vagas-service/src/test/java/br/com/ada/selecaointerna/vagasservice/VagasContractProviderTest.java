package br.com.ada.selecaointerna.vagasservice;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@Provider("vagas-service")
@PactFolder("${pact.folder}")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.datasource.url=jdbc:h2:mem:vagas-pact;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class VagasContractProviderTest {
  private static final UUID VAGA = UUID.fromString("d71c12f0-2fd9-4b70-b19e-89d539be96b0");
  @LocalServerPort int port;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  void destino(PactVerificationContext context) {
    context.setTarget(new HttpTestTarget("localhost", port));
  }

  @TestTemplate
  @ExtendWith(PactVerificationInvocationContextProvider.class)
  void verificaContratoVersionadoContraAplicacaoReal(PactVerificationContext context) {
    context.verifyInteraction();
  }

  @State("vaga aberta com requisitos")
  public void aberta() { preparar("ABERTA"); }

  @State("vaga em avaliacao com requisitos")
  public void emAvaliacao() { preparar("EM_AVALIACAO"); }

  @State({"vaga numerica inexistente", "vaga UUID inexistente"})
  public void inexistente() { limparFixtures(); }

  private void preparar(String status) {
    limparFixtures();
    jdbc.update("INSERT INTO vagas(id_vaga, vaga_id, nome_vaga, status) VALUES (?, ?, ?, ?)",
        900001L, VAGA, "Java", status);
    jdbc.update("""
        INSERT INTO requisitos_vaga(vaga_id, ordem, tipo, palavra_chave, obrigatorio, pontos, meses_minimos)
        VALUES (?, 0, 'EXPERIENCIA_INTERNA', 'Java', true, 10, 6)
        """, 900001L);
  }

  private void limparFixtures() {
    jdbc.update("DELETE FROM requisitos_vaga WHERE vaga_id IN (900001, 900099)");
    jdbc.update("DELETE FROM vagas WHERE id_vaga IN (900001, 900099)");
  }
}
