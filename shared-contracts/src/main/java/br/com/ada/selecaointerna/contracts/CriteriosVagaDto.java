package br.com.ada.selecaointerna.contracts;

import java.util.List;
import java.util.UUID;

public record CriteriosVagaDto(UUID vagaId, String titulo, StatusVaga status,
                               List<RequisitoDto> requisitos) {}