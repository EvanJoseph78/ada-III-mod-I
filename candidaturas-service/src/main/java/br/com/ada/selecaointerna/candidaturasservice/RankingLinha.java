package br.com.ada.selecaointerna.candidaturasservice;

import java.time.Instant;
import java.util.UUID;

public record RankingLinha(UUID candidaturaId, UUID empregadoId, Long matriculaEmpregado,
                           int pontuacao, Instant inscritaEm) {}
