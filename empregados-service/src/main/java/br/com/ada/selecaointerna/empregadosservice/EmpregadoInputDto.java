package br.com.ada.selecaointerna.empregadosservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

public record EmpregadoInputDto(@NotNull @Positive Long matriculaEmpregado,
                               @NotBlank @Size(max = 255) String nomeEmpregado,
                               List<@NotNull @Valid ItemPerfilInput> itens) {
  public EmpregadoInputDto {
    itens = itens == null ? List.of() : itens;
  }
}
