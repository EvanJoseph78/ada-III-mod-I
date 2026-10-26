package br.com.ada.selecaointerna.empregadosservice.service;

import br.com.ada.selecaointerna.contracts.*;
import br.com.ada.selecaointerna.empregadosservice.entity.domain.Empregado;
import br.com.ada.selecaointerna.empregadosservice.entity.domain.ItemPerfil;
import br.com.ada.selecaointerna.empregadosservice.interfaces.dto.*;
import br.com.ada.selecaointerna.empregadosservice.repository.EmpregadoRepository;
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
    return repository.buscarTodos().stream().map(EmpregadoOutputDto::new).toList();
  }

  public EmpregadoOutputDto obterEmpregado(Long matricula) {
    return new EmpregadoOutputDto(buscarPorMatricula(matricula));
  }

  public PerfilProfissionalDto obterPerfil(UUID id) {
    Empregado empregado = repository.buscarPorId(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empregado nao encontrado"));
    EmpregadoOutputDto dto = new EmpregadoOutputDto(empregado);
    return new PerfilProfissionalDto(id, empregado.nome(), dto.itens());
  }

  public EmpregadoOutputDto obterCurriculo(Long matricula, boolean profissional) {
    Empregado empregado = buscarPorMatricula(matricula);
    List<ItemPerfil> itens = empregado.itens().stream().filter(item -> {
      boolean experiencia = item.tipo() == TipoItem.EXPERIENCIA_INTERNA
          || item.tipo() == TipoItem.EXPERIENCIA_EXTERNA;
      return profissional == experiencia;
    }).toList();
    return new EmpregadoOutputDto(new Empregado(empregado.empregadoId(), empregado.matricula(), empregado.nome(), itens));
  }

  @Transactional
  public EmpregadoOutputDto cadastrarEmpregado(EmpregadoInputDto input) {
    Empregado empregado = new Empregado(UUID.randomUUID(), input.matriculaEmpregado(), input.nomeEmpregado(),
        input.itens().stream().map(item -> new ItemPerfil(item.tipo(), item.descricao(), item.horas(), item.meses())).toList());
    try {
      return new EmpregadoOutputDto(repository.salvarEmpregado(empregado));
    } catch (DataIntegrityViolationException e) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Matricula ja cadastrada", e);
    }
  }

  private Empregado buscarPorMatricula(Long matricula) {
    return repository.buscarPorMatricula(matricula)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empregado nao encontrado"));
  }
}
