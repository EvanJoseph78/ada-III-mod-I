package br.com.ada.selecaointerna.empregadosservice.entity.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Empregado {

  private Long matricula;
  private String nome;

}
