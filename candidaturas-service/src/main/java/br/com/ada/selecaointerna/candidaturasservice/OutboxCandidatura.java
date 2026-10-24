package br.com.ada.selecaointerna.candidaturasservice;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_candidaturas")
@Getter
@NoArgsConstructor
public class OutboxCandidatura {
  @Id
  private UUID eventId;
  @Column(nullable = false, unique = true)
  private UUID candidaturaId;
  @Column(nullable = false, columnDefinition = "text")
  private String payload;
  @Column(nullable = false)
  private Instant criadoEm;
  private Instant publicadoEm;

  public OutboxCandidatura(UUID eventId, Candidatura candidatura) {
    this.eventId = eventId;
    candidaturaId = candidatura.getCandidaturaId();
    payload = candidatura.getEventoCandidatura();
    criadoEm = candidatura.getRealizadaEm();
  }

  public void confirmarPublicacao() { publicadoEm = Instant.now(); }
}
