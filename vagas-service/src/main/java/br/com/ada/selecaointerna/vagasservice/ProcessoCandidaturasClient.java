package br.com.ada.selecaointerna.vagasservice;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ProcessoCandidaturasClient {
  private final RestClient client;

  public ProcessoCandidaturasClient(RestClient.Builder builder,
      @Value("${integracoes.candidaturas-url:http://localhost:8083}") String url,
      @Value("${integracoes.timeout-ms:2000}") int timeout) {
    var factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(timeout); factory.setReadTimeout(timeout);
    client = builder.clone().requestFactory(factory).baseUrl(url).build();
  }

  public void encerrar(UUID vagaId, Long idVaga) {
    executar(vagaId, idVaga, () -> client.post().uri("/api/v1/candidaturas/processos/{id}/encerramento", vagaId)
        .body(Map.of("idVaga", idVaga)).retrieve().body(Prontidao.class));
  }

  public void exigirProntidao(UUID vagaId, Long idVaga) {
    var resposta = executar(vagaId, idVaga, () -> client.get()
        .uri("/api/v1/candidaturas/processos/{id}/prontidao", vagaId).retrieve().body(Prontidao.class));
    if (!resposta.apto() || !resposta.inscricoes().equals(resposta.avaliacoes())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Ainda existem inscricoes sem avaliacao");
    }
  }

  private Prontidao executar(UUID vagaId, Long idVaga, Supplier<Prontidao> consulta) {
    try {
      var resposta = consulta.get();
      if (resposta == null || !vagaId.equals(resposta.vagaId()) || !idVaga.equals(resposta.idVaga())
          || resposta.inscricoes() == null || resposta.avaliacoes() == null || resposta.apto() == null
          || resposta.inscricoes() < 0 || resposta.avaliacoes() < 0) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Prontidao inconsistente");
      }
      return resposta;
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() == 404 || e.getStatusCode().value() == 409) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Processo de inscricoes nao permite a operacao", e);
      }
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Candidaturas indisponivel", e);
    } catch (ResourceAccessException e) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Falha ou timeout ao consultar Candidaturas", e);
    } catch (RestClientException e) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Resposta de Candidaturas invalida", e);
    }
  }

  private record Prontidao(UUID vagaId, Long idVaga, Long inscricoes, Long avaliacoes, Boolean apto) {}
}
