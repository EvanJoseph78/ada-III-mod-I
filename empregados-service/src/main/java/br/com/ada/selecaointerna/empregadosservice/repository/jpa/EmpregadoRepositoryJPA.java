package br.com.ada.selecaointerna.empregadosservice.repository.jpa;

import br.com.ada.selecaointerna.empregadosservice.entity.infra.EmpregadoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface EmpregadoRepositoryJPA extends JpaRepository<EmpregadoEntity, UUID> {
  Optional<EmpregadoEntity> findByMatriculaEmpregado(Long matricula);
}
