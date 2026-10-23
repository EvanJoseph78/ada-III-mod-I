package br.com.ada.selecaointerna.empregadosservice.service;

import br.com.ada.selecaointerna.empregadosservice.entity.domain.Empregado;
import br.com.ada.selecaointerna.empregadosservice.interfaces.dto.EmpregadoInputDto;
import br.com.ada.selecaointerna.empregadosservice.interfaces.dto.EmpregadoOutputDto;
import br.com.ada.selecaointerna.empregadosservice.repository.EmpregadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmpregadosService {

  @Autowired
  EmpregadoRepository empregadoRepository;

  public List<EmpregadoOutputDto> obterTodosEmpregados() {
    List<EmpregadoOutputDto> empregadoOutputDtoList = new ArrayList<>();
    for(Empregado e : empregadoRepository.buscarTodos()){
      empregadoOutputDtoList.add(new EmpregadoOutputDto(e));
    }

    return empregadoOutputDtoList;
  }

  public EmpregadoOutputDto obterEmpregado(Long matriculaEmpregado) {
      Empregado empregado = empregadoRepository.buscarPorMatricula(matriculaEmpregado);
      return new EmpregadoOutputDto(empregado);
  }

  public EmpregadoOutputDto cadastrarEmpregado(EmpregadoInputDto empregadoInputDto) {
    Empregado empregado = Empregado.builder()
        .matricula(empregadoInputDto.getMatriculaEmpregado())
        .nome(empregadoInputDto.getNomeEmpregado())
      .build();
    empregadoRepository.salvarEmpregado(empregado);
    return new EmpregadoOutputDto(empregado);
  }

}
