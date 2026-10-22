package br.com.ada.selecaointerna.vagasservice;

import lombok.Data;

@Data
public class VagaOutputDto {

  private Long idVaga;
  private String nomeVaga;

  public VagaOutputDto(Vaga vaga) {
    this.idVaga = vaga.getIdVaga();
    this.nomeVaga = vaga.getNomeVaga();
  }

}
