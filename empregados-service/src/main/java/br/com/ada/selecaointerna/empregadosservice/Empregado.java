package br.com.ada.selecaointerna.empregadosservice;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Empregado {

  private Long matriculaEmpregado;
  private String nomeEmpregado;

}
