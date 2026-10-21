package br.com.ada.selecaointerna.contracts;

import java.time.Instant;
import java.util.UUID;

public record ResultadoPublicado(UUID eventId, Instant ocorridoEm, UUID vagaId) {}