package br.com.ada.selecaointerna.vagasservice.dtos;

import br.com.ada.selecaointerna.vagasservice.models.Vaga;
import lombok.Data;

import java.util.UUID;

@Data
public class VagaOutputDto {

  private UUID idVaga;
  private String nomeVaga;

  public VagaOutputDto(Vaga vaga) {
    this.idVaga = vaga.getIdVaga();
    this.nomeVaga = vaga.getNomeVaga();
  }

}
