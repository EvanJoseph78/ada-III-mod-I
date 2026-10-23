package br.com.ada.selecaointerna.vagasservice.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "TB_VAGAS")
public class Vaga {

  @Id
  @Column(name = "ID_VAGA", nullable = false, unique = true)
  private UUID idVaga;

  @Column(name = "NOME_VAGA", nullable = false)
  private String nomeVaga;

}
