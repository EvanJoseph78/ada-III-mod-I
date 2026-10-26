package br.com.ada.selecaointerna.empregadosservice.interfaces.dto;

import br.com.ada.selecaointerna.contracts.ItemPerfilDto;
import br.com.ada.selecaointerna.empregadosservice.entity.domain.Empregado;
import java.util.List;
import java.util.UUID;

public record EmpregadoOutputDto(UUID empregadoId, Long matriculaEmpregado,
                                 String nomeEmpregado, List<ItemPerfilDto> itens) {
  public EmpregadoOutputDto(Empregado empregado) {
    this(empregado.empregadoId(), empregado.matricula(), empregado.nome(),
        empregado.itens().stream().map(item -> new ItemPerfilDto(
            item.tipo(), item.descricao(), item.horas(), item.meses())).toList());
  }
}
