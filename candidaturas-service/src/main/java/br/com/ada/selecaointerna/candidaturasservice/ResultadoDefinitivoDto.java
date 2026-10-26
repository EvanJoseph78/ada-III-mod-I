package br.com.ada.selecaointerna.candidaturasservice;

import br.com.ada.selecaointerna.contracts.ResultadoPublicado;

public record ResultadoDefinitivoDto(ResultadoPublicado publicacao, RankingDto ranking,
                                     long inscricoes, long avaliacoes) {}
