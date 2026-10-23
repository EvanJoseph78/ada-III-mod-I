package br.com.ada.selecaointerna.empregadosservice;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "empregados")
@Getter
@NoArgsConstructor
public class Empregado {
  @Id
  private UUID empregadoId;
  @Column(nullable = false, unique = true)
  private Long matriculaEmpregado;
  @Column(nullable = false)
  private String nomeEmpregado;
  @ElementCollection
  @CollectionTable(name = "itens_perfil", joinColumns = @JoinColumn(name = "empregado_id"))
  @OrderColumn(name = "ordem")
  private List<ItemPerfil> itens = new ArrayList<>();

  public Empregado(EmpregadoInputDto input) {
    empregadoId = UUID.randomUUID();
    matriculaEmpregado = input.matriculaEmpregado();
    nomeEmpregado = input.nomeEmpregado();
    itens = input.itens().stream().map(ItemPerfil::new).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
  }
}
