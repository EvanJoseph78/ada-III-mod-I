package br.com.ada.selecaointerna.empregadosservice;

import lombok.Data;

@Data
public class EmpregadoOutputDto {

  private Long matriculaEmpregado;
  private String nomeEmpregado;

  public EmpregadoOutputDto(Empregado empregado) {
    this.matriculaEmpregado = empregado.getMatriculaEmpregado();
    this.nomeEmpregado = empregado.getNomeEmpregado();
  }

}
