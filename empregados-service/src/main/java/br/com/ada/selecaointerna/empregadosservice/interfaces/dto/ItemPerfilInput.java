package br.com.ada.selecaointerna.empregadosservice.interfaces.dto;

import br.com.ada.selecaointerna.contracts.TipoItem;
import jakarta.validation.constraints.*;

public record ItemPerfilInput(@NotNull TipoItem tipo, @NotBlank @Size(max = 255) String descricao,
                              @PositiveOrZero Integer horas, @PositiveOrZero Integer meses) {}
