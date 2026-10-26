package br.com.ada.selecaointerna.candidaturasservice;

import br.com.ada.selecaointerna.contracts.*;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Component
public class DadosCandidaturaClient {
  private final RestClient empregados;
  private final RestClient vagas;

  public DadosCandidaturaClient(RestClient.Builder builder,
      @Value("${integracoes.empregados-url:http://localhost:8081}") String empregadosUrl,
      @Value("${integracoes.vagas-url:http://localhost:8082}") String vagasUrl,
      @Value("${integracoes.timeout-ms:2000}") int timeoutMs) {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(timeoutMs);
    factory.setReadTimeout(timeoutMs);
    empregados = builder.clone().requestFactory(factory).baseUrl(empregadosUrl).build();
    vagas = builder.clone().requestFactory(factory).baseUrl(vagasUrl).build();
  }

  public Dados obter(CandidaturaInputDto input) {
    try {
      EmpregadoRef empregado = empregados.get().uri("/api/v1/empregados/{matricula}", input.matriculaEmpregado())
          .retrieve().body(EmpregadoRef.class);
      VagaRef vaga = vagas.get().uri("/api/v1/vagas/{id}", input.idVaga()).retrieve().body(VagaRef.class);
      if (empregado == null || empregado.empregadoId() == null || vaga == null || vaga.vagaId() == null) {
        throw invalido();
      }
      PerfilProfissionalDto perfil = empregados.get().uri("/api/v1/empregados/{id}/perfil", empregado.empregadoId())
          .retrieve().body(PerfilProfissionalDto.class);
      CriteriosVagaDto criterios = vagas.get().uri("/api/v1/vagas/{id}/criterios", vaga.vagaId())
          .retrieve().body(CriteriosVagaDto.class);
      if (perfil == null || criterios == null || !empregado.empregadoId().equals(perfil.empregadoId())
          || !vaga.vagaId().equals(criterios.vagaId()) || perfil.nome() == null || criterios.titulo() == null
          || perfil.itens() == null || criterios.requisitos() == null || criterios.status() == null
          || perfil.itens().stream().anyMatch(item -> item == null || item.tipo() == null || item.descricao() == null
              || negativo(item.horas()) || negativo(item.meses()))
          || criterios.requisitos().stream().anyMatch(req -> req == null || req.tipo() == null
              || req.palavraChave() == null || req.palavraChave().isBlank() || req.pontos() < 0 || negativo(req.mesesMinimos()))) {
        throw invalido();
      }
      if (criterios.status() != StatusVaga.ABERTA) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Vaga nao esta aberta para inscricoes");
      }
      return new Dados(new PerfilProfissionalDto(perfil.empregadoId(), perfil.nome(), List.copyOf(perfil.itens())),
          new CriteriosVagaDto(criterios.vagaId(), criterios.titulo(), criterios.status(), List.copyOf(criterios.requisitos())));
    } catch (RestClientResponseException e) {
      if (e.getStatusCode().value() == 404) {
        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Empregado ou vaga inexistente", e);
      }
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servico de consulta indisponivel", e);
    } catch (ResourceAccessException e) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Falha ou timeout ao consultar empregado/vaga", e);
    } catch (RestClientException e) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Resposta de consulta invalida", e);
    }
  }

  private static boolean negativo(Integer valor) { return valor != null && valor < 0; }
  private static ResponseStatusException invalido() {
    return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Perfil ou criterios inconsistentes");
  }

  public record Dados(PerfilProfissionalDto perfil, CriteriosVagaDto criterios) {}
  private record EmpregadoRef(UUID empregadoId) {}
  private record VagaRef(UUID vagaId) {}
}
