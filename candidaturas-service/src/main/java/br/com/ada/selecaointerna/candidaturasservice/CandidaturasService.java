package br.com.ada.selecaointerna.candidaturasservice;

import java.util.UUID;
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

  @Transactional
  public CandidaturaOutputDto realizarCandidatura(CandidaturaInputDto input) {
    try {
      return new CandidaturaOutputDto(repository.saveAndFlush(new Candidatura(input)));
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Candidatura ja registrada para empregado e vaga", e);
    }
  }

  public CandidaturaOutputDto obterCandidatura(UUID id) {
    return repository.findById(id).map(CandidaturaOutputDto::new)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidatura nao encontrada"));
  }
}
