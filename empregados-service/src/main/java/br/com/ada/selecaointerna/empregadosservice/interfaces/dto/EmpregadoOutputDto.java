package br.com.ada.selecaointerna.empregadosservice.interfaces.dto;

import br.com.ada.selecaointerna.empregadosservice.entity.domain.Empregado;
import lombok.Data;

@Data
public class EmpregadoOutputDto {

  private Long matricula;
  private String nome;

  public EmpregadoOutputDto(Empregado empregado) {
    this.matricula = empregado.getMatricula();
    this.nome = empregado.getNome();
  }

}
