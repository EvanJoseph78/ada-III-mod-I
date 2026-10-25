package br.com.ada.selecaointerna.vagasservice;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class ProcessoCandidaturasClientTest {
  HttpServer server;
  ProcessoCandidaturasClient client;
  UUID vaga = UUID.randomUUID();
  volatile String body;
  volatile String recebido;
  volatile int status = 200, atraso;

  @BeforeEach
  void iniciar() throws Exception {
    body = resposta(false, 1, 0);
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      recebido = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      try {
        if (atraso > 0) Thread.sleep(atraso);
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
      } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
      finally { exchange.close(); }
    });
    server.start();
    client = new ProcessoCandidaturasClient(RestClient.builder(), "http://127.0.0.1:" + server.getAddress().getPort(), 150);
  }
  @AfterEach void parar() { server.stop(0); }

  @Test
  void fechamentoEnviaIdEPublicacaoExigeContagensIguais() {
    client.encerrar(vaga, 7L);
    assertThat(recebido).contains("\"idVaga\":7");
    erro(409);
    body = resposta(true, 1, 0); erro(409);
    body = resposta(true, 1, 1); client.exigirProntidao(vaga, 7L);
  }

  @Test
  void errosRemotosEContratosInconsistentesNaoLiberamPublicacao() {
    status = 500; erro(503);
    status = 404; erro(409);
    status = 200; body = "json-invalido"; erro(502);
    body = "{\"vagaId\":\"%s\",\"idVaga\":7,\"apto\":true}".formatted(vaga); erro(502);
    body = resposta(true, 0, 0).replace(vaga.toString(), UUID.randomUUID().toString()); erro(502);
  }

  @Test
  void timeoutRemotoRetornaIndisponibilidade() { atraso = 500; erro(503); }

  private void erro(int esperado) {
    assertThatExceptionOfType(ResponseStatusException.class).isThrownBy(() -> client.exigirProntidao(vaga, 7L))
        .satisfies(e -> assertThat(e.getStatusCode().value()).isEqualTo(esperado));
  }
  private String resposta(boolean apto, int inscricoes, int avaliacoes) {
    return "{\"vagaId\":\"%s\",\"idVaga\":7,\"inscricoes\":%d,\"avaliacoes\":%d,\"apto\":%s}"
        .formatted(vaga, inscricoes, avaliacoes, apto);
  }
}
