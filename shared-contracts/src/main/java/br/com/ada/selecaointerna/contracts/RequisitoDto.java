package br.com.ada.selecaointerna.contracts;

public record RequisitoDto(TipoItem tipo, String palavraChave, boolean obrigatorio,
                           int pontos, Integer mesesMinimos) {}