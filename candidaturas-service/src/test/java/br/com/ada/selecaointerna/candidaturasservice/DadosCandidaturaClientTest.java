package br.com.ada.selecaointerna.candidaturasservice;

import br.com.ada.selecaointerna.contracts.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class DadosCandidaturaClientTest {
  private HttpServer server;
  private DadosCandidaturaClient client;
  private final Map<String, Resposta> respostas = new ConcurrentHashMap<>();
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  private final UUID empregado = UUID.randomUUID();
  private final UUID vaga = UUID.randomUUID();

  @BeforeEach
  void configurar() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      Resposta resposta = respostas.getOrDefault(exchange.getRequestURI().getPath(), new Resposta(404, "{}", 0));
      try {
        if (resposta.atraso() > 0) Thread.sleep(resposta.atraso());
        byte[] bytes = resposta.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(resposta.status(), bytes.length);
        exchange.getResponseBody().write(bytes);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } finally { exchange.close(); }
    });
    respostas.put("/api/v1/empregados/123", json(Map.of("empregadoId", empregado, "nomeEmpregado", "Ana")));
    respostas.put("/api/v1/vagas/1", json(Map.of("vagaId", vaga, "nomeVaga", "Java")));
    respostas.put("/api/v1/empregados/" + empregado + "/perfil", json(new PerfilProfissionalDto(empregado, "Ana",
        List.of(new ItemPerfilDto(TipoItem.CURSO, "Java", 40, null)))));
    respostas.put("/api/v1/vagas/" + vaga + "/criterios", criterios(StatusVaga.ABERTA));
    server.start();
    String url = "http://127.0.0.1:" + server.getAddress().getPort();
    client = new DadosCandidaturaClient(RestClient.builder(), url, url, 300);
  }

  @AfterEach
  void encerrar() { server.stop(0); }

  @Test
  void resolveIdsEObtemSnapshotsImutaveisPorHttp() {
    var dados = client.obter(new CandidaturaInputDto(123L, 1L));
    assertThat(dados.perfil().empregadoId()).isEqualTo(empregado);
    assertThat(dados.criterios().vagaId()).isEqualTo(vaga);
    assertThat(dados.perfil().itens().getFirst().descricao()).isEqualTo("Java");
    assertThatThrownBy(() -> dados.perfil().itens().clear()).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void referenciaInexistenteRetorna422() {
    respostas.put("/api/v1/empregados/123", new Resposta(404, "{}", 0));
    verificarStatus(422);
  }

  @Test
  void vagaFechadaRetorna409() throws Exception {
    respostas.put("/api/v1/vagas/" + vaga + "/criterios", criterios(StatusVaga.EM_AVALIACAO));
    verificarStatus(409);
  }

  @Test
  void falhaRemotaRetorna503() {
    respostas.put("/api/v1/vagas/1", new Resposta(500, "{}", 0));
    verificarStatus(503);
  }

  @Test
  void timeoutRetorna503() {
    respostas.put("/api/v1/empregados/123", new Resposta(200, "{}", 1000));
    verificarStatus(503);
  }

  @Test
  void snapshotComIdentidadeIncorretaRetorna502() throws Exception {
    respostas.put("/api/v1/empregados/" + empregado + "/perfil",
        json(new PerfilProfissionalDto(UUID.randomUUID(), "Outro", List.of())));
    verificarStatus(502);
  }

  private void verificarStatus(int status) {
    assertThatExceptionOfType(ResponseStatusException.class)
        .isThrownBy(() -> client.obter(new CandidaturaInputDto(123L, 1L)))
        .satisfies(e -> assertThat(e.getStatusCode().value()).isEqualTo(status));
  }

  private Resposta criterios(StatusVaga status) throws Exception {
    return json(new CriteriosVagaDto(vaga, "Java", status,
        List.of(new RequisitoDto(TipoItem.CURSO, "Java", true, 10, null))));
  }
  private Resposta json(Object value) throws Exception { return new Resposta(200, mapper.writeValueAsString(value), 0); }
  private record Resposta(int status, String body, long atraso) {}
}
