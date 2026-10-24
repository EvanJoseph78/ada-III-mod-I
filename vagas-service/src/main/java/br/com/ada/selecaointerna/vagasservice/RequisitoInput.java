package br.com.ada.selecaointerna.vagasservice;

import br.com.ada.selecaointerna.contracts.TipoItem;
import jakarta.validation.constraints.*;

public record RequisitoInput(@NotNull TipoItem tipo, @NotBlank @Size(max = 255) String palavraChave,
                             boolean obrigatorio, @NotNull @PositiveOrZero Integer pontos,
                             @PositiveOrZero Integer mesesMinimos) {}
