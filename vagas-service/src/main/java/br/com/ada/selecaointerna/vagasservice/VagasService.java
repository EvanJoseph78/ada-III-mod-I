package br.com.ada.selecaointerna.vagasservice;

import br.com.ada.selecaointerna.contracts.CriteriosVagaDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VagasService {
  private final VagaRepository repository;

  public List<VagaOutputDto> obterTodasVagas() {
    return repository.findAll().stream().map(VagaOutputDto::new).toList();
  }

  public VagaOutputDto obterVaga(Long id) { return new VagaOutputDto(buscar(id)); }

  public CriteriosVagaDto obterCriterios(UUID id) {
    Vaga vaga = repository.findByVagaId(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vaga nao encontrada"));
    return new CriteriosVagaDto(id, vaga.getNomeVaga(), vaga.getStatus(),
        vaga.getRequisitos().stream().map(Requisito::toDto).toList());
  }

  @Transactional
  public VagaOutputDto cadastrarVaga(VagaInputDto input) {
    return new VagaOutputDto(repository.saveAndFlush(new Vaga(input)));
  }

  @Transactional
  public void removerVaga(Long id) { repository.delete(buscar(id)); }

  private Vaga buscar(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vaga nao encontrada"));
  }
}
