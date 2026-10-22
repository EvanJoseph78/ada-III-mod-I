package br.com.ada.selecaointerna.vagasservice;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Vaga {

  private Long idVaga;
  private String nomeVaga;

}
