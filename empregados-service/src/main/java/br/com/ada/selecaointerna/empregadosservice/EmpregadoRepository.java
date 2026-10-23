package br.com.ada.selecaointerna.empregadosservice;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpregadoRepository extends JpaRepository<Empregado, UUID> {
  Optional<Empregado> findByMatriculaEmpregado(Long matricula);
}
