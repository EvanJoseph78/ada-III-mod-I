package br.com.ada.selecaointerna.candidaturasservice;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "candidaturas", uniqueConstraints = @UniqueConstraint(columnNames = {"matricula_empregado", "id_vaga"}))
@Getter
@NoArgsConstructor
public class Candidatura {
  @Id
  private UUID candidaturaId;
  @Column(nullable = false)
  private Long matriculaEmpregado;
  @Column(nullable = false)
  private Long idVaga;
  @Column(nullable = false)
  private Instant realizadaEm;

  public Candidatura(CandidaturaInputDto input) {
    candidaturaId = UUID.randomUUID(); matriculaEmpregado = input.matriculaEmpregado();
    idVaga = input.idVaga(); realizadaEm = Instant.now();
  }
}
