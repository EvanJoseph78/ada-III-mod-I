package br.com.ada.selecaointerna.empregadosservice.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "TB_EMPREGADOS")
public class Empregado {

  @Id
  @Column(name = "ID_EMPREGADO", nullable = false, unique = true)
  private UUID idEmpregado;

  @Column(name = "MATRICULA_EMPREGADO", nullable = false, unique = true)
  private Long matriculaEmpregado;

  @Column(name = "NOME_EMPREGADO", nullable = false)
  private String nomeEmpregado;

}
