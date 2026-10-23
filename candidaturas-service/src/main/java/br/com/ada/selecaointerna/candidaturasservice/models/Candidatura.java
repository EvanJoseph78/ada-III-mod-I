package br.com.ada.selecaointerna.candidaturasservice.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "TB_CANDIDATURAS",
    uniqueConstraints = @UniqueConstraint(columnNames = {"ID_EMPREGADO", "ID_VAGA"})
)
public class Candidatura {

  @Id
  @Column(name = "ID_CANDIDATURA", nullable = false, unique = true)
  private UUID idCandidatura;

  @Column(name = "ID_EMPREGADO", nullable = false)
  private UUID idEmpregado;

  @Column(name = "ID_VAGA", nullable = false)
  private UUID idVaga;

}
