package br.com.ada.selecaointerna.empregadosservice.entity.infra;

import br.com.ada.selecaointerna.contracts.TipoItem;
import br.com.ada.selecaointerna.empregadosservice.entity.domain.ItemPerfil;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
public class ItemPerfilEntity {
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TipoItem tipo;
  @Column(nullable = false)
  private String descricao;
  private Integer horas;
  private Integer meses;

  public ItemPerfilEntity(ItemPerfil item) {
    tipo = item.tipo(); descricao = item.descricao(); horas = item.horas(); meses = item.meses();
  }

  public ItemPerfil toDomain() { return new ItemPerfil(tipo, descricao, horas, meses); }
}
