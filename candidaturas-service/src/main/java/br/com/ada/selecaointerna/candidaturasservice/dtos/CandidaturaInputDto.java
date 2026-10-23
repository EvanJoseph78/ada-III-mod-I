package br.com.ada.selecaointerna.candidaturasservice.dtos;

import lombok.Data;

import java.util.UUID;

@Data
public class CandidaturaInputDto {

  private UUID idEmpregado;
  private UUID idVaga;

}
