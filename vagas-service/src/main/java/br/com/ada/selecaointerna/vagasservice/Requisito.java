package br.com.ada.selecaointerna.vagasservice;

import br.com.ada.selecaointerna.contracts.RequisitoDto;
import br.com.ada.selecaointerna.contracts.TipoItem;
import jakarta.persistence.*;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
public class Requisito {
  @Enumerated(EnumType.STRING) @Column(nullable = false)
  private TipoItem tipo;
  @Column(nullable = false)
  private String palavraChave;
  private boolean obrigatorio;
  private int pontos;
  private Integer mesesMinimos;

  public Requisito(RequisitoInput input) {
    tipo = input.tipo(); palavraChave = input.palavraChave(); obrigatorio = input.obrigatorio();
    pontos = input.pontos(); mesesMinimos = input.mesesMinimos();
  }

  public RequisitoDto toDto() { return new RequisitoDto(tipo, palavraChave, obrigatorio, pontos, mesesMinimos); }
}
