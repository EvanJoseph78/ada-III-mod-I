package br.com.ada.selecaointerna.candidaturasservice;

import java.util.UUID;
import br.com.ada.selecaointerna.contracts.CandidaturaRealizada;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CandidaturasService {
  private final CandidaturaRepository repository;
  private final DadosCandidaturaClient dadosClient;
  private final ObjectMapper mapper;
  private final OutboxRepository outboxRepository;
  private final AvaliacaoRepository avaliacaoRepository;
  private final ProcessoVagaService processos;

  @Transactional
  public CandidaturaOutputDto realizarCandidatura(CandidaturaInputDto input) {
    if (repository.existsByMatriculaEmpregadoAndIdVaga(input.matriculaEmpregado(), input.idVaga())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Candidatura ja registrada para empregado e vaga");
    }
    DadosCandidaturaClient.Dados dados = dadosClient.obter(input);
    processos.validarInscricao(dados.criterios().vagaId(), input.idVaga());
    Candidatura candidatura = new Candidatura(input);
    CandidaturaRealizada evento = new CandidaturaRealizada(UUID.randomUUID(), candidatura.getRealizadaEm(),
        candidatura.getCandidaturaId(), dados.criterios().vagaId(), dados.perfil().empregadoId(), dados.perfil(), dados.criterios());
    try {
      candidatura.registrarEvento(mapper.writeValueAsString(evento));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Falha ao serializar snapshots da candidatura", e);
    }
    try {
      repository.saveAndFlush(candidatura);
      outboxRepository.saveAndFlush(new OutboxCandidatura(evento.eventId(), candidatura));
      return new CandidaturaOutputDto(candidatura);
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Candidatura ja registrada para empregado e vaga", e);
    }
  }

  public CandidaturaOutputDto obterCandidatura(UUID id) {
    return repository.findById(id).map(candidatura -> new CandidaturaOutputDto(candidatura,
        avaliacaoRepository.findById(id).orElse(null)))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidatura nao encontrada"));
  }
}
