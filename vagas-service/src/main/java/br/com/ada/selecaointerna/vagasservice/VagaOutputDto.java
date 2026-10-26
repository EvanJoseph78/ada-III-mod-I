package br.com.ada.selecaointerna.vagasservice;

import br.com.ada.selecaointerna.contracts.*;
import java.util.List;
import java.util.UUID;

public record VagaOutputDto(Long idVaga, UUID vagaId, String nomeVaga,
                            StatusVaga status, List<RequisitoDto> requisitos) {
  public VagaOutputDto(Vaga vaga) {
    this(vaga.getIdVaga(), vaga.getVagaId(), vaga.getNomeVaga(), vaga.getStatus(),
        vaga.getRequisitos().stream().map(Requisito::toDto).toList());
  }
}
