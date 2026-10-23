package br.com.ada.selecaointerna.vagasservice;

import br.com.ada.selecaointerna.contracts.StatusVaga;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "vagas")
@Getter
@NoArgsConstructor
public class Vaga {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long idVaga;
  @Column(nullable = false, unique = true)
  private UUID vagaId;
  @Column(nullable = false)
  private String nomeVaga;
  @Enumerated(EnumType.STRING) @Column(nullable = false)
  private StatusVaga status;
  @ElementCollection
  @CollectionTable(name = "requisitos_vaga", joinColumns = @JoinColumn(name = "vaga_id"))
  @OrderColumn(name = "ordem")
  private List<Requisito> requisitos = new ArrayList<>();

  public Vaga(VagaInputDto input) {
    vagaId = UUID.randomUUID(); nomeVaga = input.nomeVaga(); status = StatusVaga.ABERTA;
    requisitos = input.requisitos().stream().map(Requisito::new)
        .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
  }
}
