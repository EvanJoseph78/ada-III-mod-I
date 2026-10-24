package br.com.ada.selecaointerna.empregadosservice.entity.infra;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Entity
@Table(name = "empregado")
@Builder
@Data
@AllArgsConstructor
public class EmpregadoEntity {
    @Id
    @Column(name = "matricula")
    private Long matricula;

    @Column(name = "nome")
    private String nome;
}
