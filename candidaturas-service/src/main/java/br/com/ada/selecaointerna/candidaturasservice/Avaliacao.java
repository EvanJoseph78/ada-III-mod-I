package br.com.ada.selecaointerna.candidaturasservice;

import br.com.ada.selecaointerna.contracts.CandidaturaRealizada;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "avaliacoes")
@Getter
@NoArgsConstructor
public class Avaliacao {
  @Id
  private UUID candidaturaId;
  @Column(nullable = false, unique = true)
  private UUID eventId;
  @Column(nullable = false)
  private UUID empregadoId;
  @Column(nullable = false)
  private UUID vagaId;
  @Column(nullable = false)
  private boolean elegivel;
  @Column(nullable = false)
  private int pontuacao;
  @Column(nullable = false)
  private Instant avaliadaEm;

  public Avaliacao(CandidaturaRealizada evento, AvaliadorCurricular.Resultado resultado) {
    candidaturaId = evento.candidaturaId(); eventId = evento.eventId();
    empregadoId = evento.empregadoId(); vagaId = evento.vagaId();
    elegivel = resultado.elegivel(); pontuacao = resultado.pontuacao(); avaliadaEm = Instant.now();
  }
}
