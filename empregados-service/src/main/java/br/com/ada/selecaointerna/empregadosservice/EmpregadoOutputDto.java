package br.com.ada.selecaointerna.empregadosservice;

import br.com.ada.selecaointerna.contracts.ItemPerfilDto;
import java.util.List;
import java.util.UUID;

public record EmpregadoOutputDto(UUID empregadoId, Long matriculaEmpregado,
                                 String nomeEmpregado, List<ItemPerfilDto> itens) {
  public EmpregadoOutputDto(Empregado empregado) {
    this(empregado.getEmpregadoId(), empregado.getMatriculaEmpregado(), empregado.getNomeEmpregado(),
        empregado.getItens().stream().map(ItemPerfil::toDto).toList());
  }
}
