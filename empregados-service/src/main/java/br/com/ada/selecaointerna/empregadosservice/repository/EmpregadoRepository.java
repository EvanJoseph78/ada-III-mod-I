package br.com.ada.selecaointerna.empregadosservice.repository;

import br.com.ada.selecaointerna.empregadosservice.entity.domain.Empregado;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmpregadoRepository {
  Empregado salvarEmpregado(Empregado empregado);
  Optional<Empregado> buscarPorMatricula(Long matricula);
  Optional<Empregado> buscarPorId(UUID id);
  List<Empregado> buscarTodos();
}
