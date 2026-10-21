package br.com.ada.selecaointerna.contracts;

import java.time.Instant;
import java.util.UUID;

public record CandidaturaRealizada(UUID eventId, Instant ocorridoEm,
        UUID candidaturaId, UUID vagaId, UUID empregadoId,
        PerfilProfissionalDto perfilSnapshot, CriteriosVagaDto criteriosSnapshot) {}