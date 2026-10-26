package br.com.ada.selecaointerna.empregadosservice.entity.infra;

import br.com.ada.selecaointerna.empregadosservice.entity.domain.Empregado;
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
public class EmpregadoEntity {
  @Id
  private UUID empregadoId;
  @Column(nullable = false, unique = true)
  private Long matriculaEmpregado;
  @Column(nullable = false)
  private String nomeEmpregado;
  @ElementCollection
  @CollectionTable(name = "itens_perfil", joinColumns = @JoinColumn(name = "empregado_id"))
  @OrderColumn(name = "ordem")
  private List<ItemPerfilEntity> itens = new ArrayList<>();

  public EmpregadoEntity(Empregado empregado) {
    empregadoId = empregado.empregadoId();
    matriculaEmpregado = empregado.matricula();
    nomeEmpregado = empregado.nome();
    itens = new ArrayList<>(empregado.itens().stream().map(ItemPerfilEntity::new).toList());
  }

  public Empregado toDomain() {
    return new Empregado(empregadoId, matriculaEmpregado, nomeEmpregado,
        itens.stream().map(ItemPerfilEntity::toDomain).toList());
  }
}
