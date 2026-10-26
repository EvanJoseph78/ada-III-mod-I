package br.com.ada.selecaointerna.candidaturasservice;

import br.com.ada.selecaointerna.contracts.ResultadoPublicado;
import br.com.ada.selecaointerna.contracts.Topicos;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
@ConditionalOnProperty(name = "mensageria.enabled", havingValue = "true", matchIfMissing = true)
public class ResultadoKafkaListener {
  private final ObjectMapper mapper;
  private final ResultadoDefinitivoService service;
  @KafkaListener(id = "resultado-publicado", topics = Topicos.VAGAS_RESULTADOS,
      groupId = "${mensageria.grupo-resultados:resultados-publicados}")
  public void receber(String payload) {
    ResultadoPublicado evento;
    try { evento = mapper.readValue(payload, ResultadoPublicado.class); }
    catch (JsonProcessingException e) { throw new IllegalArgumentException("JSON de resultado invalido"); }
    service.processar(evento);
  }
}
