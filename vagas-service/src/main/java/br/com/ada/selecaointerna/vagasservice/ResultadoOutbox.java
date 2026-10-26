package br.com.ada.selecaointerna.vagasservice;

import br.com.ada.selecaointerna.contracts.ResultadoPublicado;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Table(name = "outbox_resultados") @Getter @NoArgsConstructor
public class ResultadoOutbox {
  @Id private UUID eventId;
  @Column(nullable = false, unique = true) private UUID vagaId;
  @Column(nullable = false, columnDefinition = "text") private String payload;
  @Column(nullable = false) private Instant criadoEm;
  private Instant publicadoEm;

  public ResultadoOutbox(ResultadoPublicado evento, String payload) {
    eventId = evento.eventId(); vagaId = evento.vagaId(); criadoEm = evento.ocorridoEm(); this.payload = payload;
  }
  public void confirmar() { publicadoEm = Instant.now(); }
}
