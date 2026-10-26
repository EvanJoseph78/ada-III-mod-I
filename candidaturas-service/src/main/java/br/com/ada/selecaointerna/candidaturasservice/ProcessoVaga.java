package br.com.ada.selecaointerna.candidaturasservice;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Table(name = "processos_vagas") @Getter @NoArgsConstructor
public class ProcessoVaga {
  @Id private UUID vagaId;
  @Column(nullable = false, unique = true) private Long idVaga;
  @Column(nullable = false) private Instant encerradaEm;
  @Column(unique = true) private UUID eventId;
  private Instant publicadoEm;
  @Column(columnDefinition = "text") private String resultadoJson;

  public ProcessoVaga(UUID vagaId, Long idVaga) {
    this.vagaId = vagaId; this.idVaga = idVaga; encerradaEm = Instant.now();
  }

  public void registrarResultado(UUID eventId, Instant publicadoEm, String json) {
    this.eventId = eventId; this.publicadoEm = publicadoEm; resultadoJson = json;
  }
}
