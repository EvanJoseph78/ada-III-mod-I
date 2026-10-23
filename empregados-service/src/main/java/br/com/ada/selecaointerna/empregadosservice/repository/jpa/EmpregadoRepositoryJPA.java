package br.com.ada.selecaointerna.empregadosservice.repository.jpa;

import br.com.ada.selecaointerna.empregadosservice.entity.infra.EmpregadoEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpregadoRepositoryJPA extends JpaRepository<EmpregadoEntity, Long> {
    Optional<EmpregadoEntity> findByMatricula(Long id);
}
