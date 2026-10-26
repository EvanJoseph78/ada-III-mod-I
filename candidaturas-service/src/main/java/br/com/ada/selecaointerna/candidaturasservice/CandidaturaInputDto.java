package br.com.ada.selecaointerna.candidaturasservice;

import jakarta.validation.constraints.*;

public record CandidaturaInputDto(@NotNull @Positive Long matriculaEmpregado,
                                  @NotNull @Positive Long idVaga) {}
