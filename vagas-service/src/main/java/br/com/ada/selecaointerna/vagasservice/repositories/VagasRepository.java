package br.com.ada.selecaointerna.vagasservice.repositories;

import br.com.ada.selecaointerna.vagasservice.models.Vaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VagasRepository extends JpaRepository<Vaga, UUID> {
}
