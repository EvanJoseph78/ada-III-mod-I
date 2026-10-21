package br.com.ada.selecaointerna.contracts;

import java.util.List;
import java.util.UUID;

public record PerfilProfissionalDto(UUID empregadoId, String nome, List<ItemPerfilDto> itens) {}