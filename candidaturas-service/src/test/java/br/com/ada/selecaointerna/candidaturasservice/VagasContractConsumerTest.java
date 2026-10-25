package br.com.ada.selecaointerna.candidaturasservice;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import br.com.ada.selecaointerna.contracts.StatusVaga;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "vagas-service", pactVersion = PactSpecVersion.V3)
class VagasContractConsumerTest {
  static final String VAGA = "d71c12f0-2fd9-4b70-b19e-89d539be96b0";
  static final String EMPREGADO = "4692f4d8-afac-47bf-9dd3-e57dd0779a46";
  private HttpServer empregados;

  @BeforeEach
  void prepararEmpregadosForaDesteContrato() throws Exception {
    empregados = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    empregados.createContext("/", exchange -> {
      String body = exchange.getRequestURI().getPath().endsWith("/perfil")
          ? "{\"empregadoId\":\"%s\",\"nome\":\"Ana\",\"itens\":[]}".formatted(EMPREGADO)
          : "{\"empregadoId\":\"%s\"}".formatted(EMPREGADO);
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      try {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
      } finally { exchange.close(); }
    });
    empregados.start();
  }

  @AfterEach
  void encerrarEmpregados() { if (empregados != null) empregados.stop(0); }

  @Pact(consumer = "candidaturas-service")
  public RequestResponsePact aberta(PactDslWithProvider builder) {
    return criterios(builder, "vaga aberta com requisitos", "ABERTA");
  }

  @Pact(consumer = "candidaturas-service")
  public RequestResponsePact fechada(PactDslWithProvider builder) {
    return criterios(builder, "vaga em avaliacao com requisitos", "EM_AVALIACAO");
  }

  private RequestResponsePact criterios(PactDslWithProvider builder, String estado, String status) {
    return builder.given(estado)
        .uponReceiving("resolver vaga numerica: " + status).path("/api/v1/vagas/900001").method("GET")
        .willRespondWith().status(200).headers(Map.of("Content-Type", "application/json"))
        .body(new PactDslJsonBody().stringValue("vagaId", VAGA))
        .given(estado)
        .uponReceiving("consultar criterios: " + status).path("/api/v1/vagas/" + VAGA + "/criterios").method("GET")
        .willRespondWith().status(200).headers(Map.of("Content-Type", "application/json"))
        .body(new PactDslJsonBody().stringValue("vagaId", VAGA).stringType("titulo", "Java")
            .stringValue("status", status).minArrayLike("requisitos", 1)
              .stringValue("tipo", "EXPERIENCIA_INTERNA").stringType("palavraChave", "Java")
              .booleanType("obrigatorio", true).integerType("pontos", 10).integerType("mesesMinimos", 6)
            .closeObject().closeArray())
        .toPact();
  }

  @Pact(consumer = "candidaturas-service")
  public RequestResponsePact inexistente(PactDslWithProvider builder) {
    return builder.given("vaga numerica inexistente")
        .uponReceiving("resolver vaga inexistente").path("/api/v1/vagas/900099").method("GET")
        .willRespondWith().status(404).toPact();
  }

  @Pact(consumer = "candidaturas-service")
  public RequestResponsePact criteriosInexistentes(PactDslWithProvider builder) {
    return builder.given("vaga aberta com requisitos")
        .uponReceiving("resolver referencia antes de remocao").path("/api/v1/vagas/900001").method("GET")
        .willRespondWith().status(200).headers(Map.of("Content-Type", "application/json"))
        .body(new PactDslJsonBody().stringValue("vagaId", VAGA))
        .given("vaga UUID inexistente")
        .uponReceiving("criterios de vaga inexistente").path("/api/v1/vagas/" + VAGA + "/criterios").method("GET")
        .willRespondWith().status(404).toPact();
  }

  @Test @PactTestFor(pactMethod = "aberta")
  void clienteRealResolveVagaEConsomeCriterios(MockServer server) {
    var dados = cliente(server).obter(new CandidaturaInputDto(123L, 900001L));
    assertThat(dados.criterios().vagaId()).isEqualTo(UUID.fromString(VAGA));
    assertThat(dados.criterios().status()).isEqualTo(StatusVaga.ABERTA);
    var requisito = dados.criterios().requisitos().getFirst();
    assertThat(requisito.pontos()).isEqualTo(10);
    assertThat(requisito.mesesMinimos()).isEqualTo(6);
    assertThat(requisito.obrigatorio()).isTrue();
  }

  @Test @PactTestFor(pactMethod = "fechada")
  void clienteRejeitaVagaQueNaoAceitaInscricoes(MockServer server) { verificarErro(server, 900001L, 409); }

  @Test @PactTestFor(pactMethod = "inexistente")
  void clienteTraduzReferenciaInexistente(MockServer server) { verificarErro(server, 900099L, 422); }

  @Test @PactTestFor(pactMethod = "criteriosInexistentes")
  void clienteTraduzCriteriosInexistentes(MockServer server) { verificarErro(server, 900001L, 422); }

  private void verificarErro(MockServer server, long vaga, int status) {
    assertThatExceptionOfType(ResponseStatusException.class)
        .isThrownBy(() -> cliente(server).obter(new CandidaturaInputDto(123L, vaga)))
        .satisfies(e -> assertThat(e.getStatusCode().value()).isEqualTo(status));
  }

  private DadosCandidaturaClient cliente(MockServer server) {
    return new DadosCandidaturaClient(RestClient.builder(),
        "http://127.0.0.1:" + empregados.getAddress().getPort(), server.getUrl(), 2000);
  }
}
