package br.com.ada.selecaointerna.empregadosservice;

import br.com.ada.selecaointerna.contracts.PerfilProfissionalDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmpregadosService {
  private final EmpregadoRepository repository;

  public List<EmpregadoOutputDto> obterTodosEmpregados() {
    return repository.findAll().stream().map(EmpregadoOutputDto::new).toList();
  }

  public EmpregadoOutputDto obterEmpregado(Long matricula) {
    return new EmpregadoOutputDto(repository.findByMatriculaEmpregado(matricula)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empregado nao encontrado")));
  }

  public PerfilProfissionalDto obterPerfil(UUID id) {
    Empregado empregado = repository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empregado nao encontrado"));
    return new PerfilProfissionalDto(id, empregado.getNomeEmpregado(),
        empregado.getItens().stream().map(ItemPerfil::toDto).toList());
  }

  @Transactional
  public EmpregadoOutputDto cadastrarEmpregado(EmpregadoInputDto input) {
    try {
      return new EmpregadoOutputDto(repository.saveAndFlush(new Empregado(input)));
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Matricula ja cadastrada", e);
    }
  }
}
