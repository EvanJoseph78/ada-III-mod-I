package br.com.ada.selecaointerna.empregadosservice.dtos;

import br.com.ada.selecaointerna.empregadosservice.models.Empregado;
import lombok.Data;

import java.util.UUID;

@Data
public class EmpregadoOutputDto {

  private UUID idEmpregado;
  private Long matriculaEmpregado;
  private String nomeEmpregado;

  public EmpregadoOutputDto(Empregado empregado) {
    this.idEmpregado = empregado.getIdEmpregado();
    this.matriculaEmpregado = empregado.getMatriculaEmpregado();
    this.nomeEmpregado = empregado.getNomeEmpregado();
  }

}
