package br.com.ada.selecaointerna.empregadosservice.entity.domain;

import br.com.ada.selecaointerna.contracts.TipoItem;

public record ItemPerfil(TipoItem tipo, String descricao, Integer horas, Integer meses) {}
