package br.com.ada.selecaointerna.empregadosservice.entity.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Certificacao {
    private Long id;
    private String descricaoCertificacao;

}
