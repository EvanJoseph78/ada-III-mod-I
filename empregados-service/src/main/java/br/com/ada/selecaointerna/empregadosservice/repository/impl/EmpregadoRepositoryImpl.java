package br.com.ada.selecaointerna.empregadosservice.repository.impl;

import br.com.ada.selecaointerna.empregadosservice.entity.domain.Empregado;
import br.com.ada.selecaointerna.empregadosservice.entity.infra.EmpregadoEntity;
import br.com.ada.selecaointerna.empregadosservice.repository.EmpregadoRepository;
import br.com.ada.selecaointerna.empregadosservice.repository.jpa.EmpregadoRepositoryJPA;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class EmpregadoRepositoryImpl implements EmpregadoRepository {
  private final EmpregadoRepositoryJPA jpaRepository;

  @Override
  public Empregado salvarEmpregado(Empregado empregado) {
    return jpaRepository.saveAndFlush(new EmpregadoEntity(empregado)).toDomain();
  }

  @Override
  public Optional<Empregado> buscarPorMatricula(Long matricula) {
    return jpaRepository.findByMatriculaEmpregado(matricula).map(EmpregadoEntity::toDomain);
  }

  @Override
  public Optional<Empregado> buscarPorId(UUID id) {
    return jpaRepository.findById(id).map(EmpregadoEntity::toDomain);
  }

  @Override
  public List<Empregado> buscarTodos() {
    return jpaRepository.findAll().stream().map(EmpregadoEntity::toDomain).toList();
  }
}
