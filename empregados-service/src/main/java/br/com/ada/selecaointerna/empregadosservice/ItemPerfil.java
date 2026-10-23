package br.com.ada.selecaointerna.empregadosservice;

import br.com.ada.selecaointerna.contracts.ItemPerfilDto;
import br.com.ada.selecaointerna.contracts.TipoItem;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
public class ItemPerfil {
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TipoItem tipo;
  @Column(nullable = false)
  private String descricao;
  private Integer horas;
  private Integer meses;

  public ItemPerfil(ItemPerfilInput input) {
    tipo = input.tipo(); descricao = input.descricao(); horas = input.horas(); meses = input.meses();
  }

  public ItemPerfilDto toDto() { return new ItemPerfilDto(tipo, descricao, horas, meses); }
}
