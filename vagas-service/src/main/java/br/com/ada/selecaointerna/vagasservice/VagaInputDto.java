package br.com.ada.selecaointerna.vagasservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record VagaInputDto(@NotBlank @Size(max = 255) String nomeVaga,
                           List<@NotNull @Valid RequisitoInput> requisitos) {
  public VagaInputDto { requisitos = requisitos == null ? List.of() : requisitos; }
}
