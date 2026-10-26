package br.com.ada.selecaointerna.empregadosservice.entity.domain;

import java.util.List;
import java.util.UUID;

public record Empregado(UUID empregadoId, Long matricula, String nome, List<ItemPerfil> itens) {
  public Empregado { itens = List.copyOf(itens); }
}
