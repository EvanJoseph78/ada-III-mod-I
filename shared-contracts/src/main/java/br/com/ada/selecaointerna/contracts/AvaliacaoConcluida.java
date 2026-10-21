package br.com.ada.selecaointerna.contracts;

import java.time.Instant;
import java.util.UUID;

public record AvaliacaoConcluida(UUID eventId, Instant ocorridoEm,
        UUID candidaturaId, UUID vagaId, UUID empregadoId,
        boolean elegivel, int pontuacao) {}