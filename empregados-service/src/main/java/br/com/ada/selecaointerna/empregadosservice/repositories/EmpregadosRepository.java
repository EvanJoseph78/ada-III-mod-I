package br.com.ada.selecaointerna.empregadosservice.repositories;

import br.com.ada.selecaointerna.empregadosservice.models.Empregado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EmpregadosRepository extends JpaRepository<Empregado, UUID> {
}
