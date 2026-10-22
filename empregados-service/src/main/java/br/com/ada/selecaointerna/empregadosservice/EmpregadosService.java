package br.com.ada.selecaointerna.empregadosservice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmpregadosService {

  private final List<Empregado> empregados = new ArrayList<>();

  public List<EmpregadoOutputDto> obterTodosEmpregados() {
    return empregados.stream().map(EmpregadoOutputDto::new).toList();
  }

  public EmpregadoOutputDto obterEmpregado(Long matriculaEmpregado) {
    return new EmpregadoOutputDto(empregados.stream().filter(vaga -> vaga.getMatriculaEmpregado().equals(matriculaEmpregado)).findFirst().get());
  }

  public EmpregadoOutputDto cadastrarEmpregado(EmpregadoInputDto empregadoInputDto) {
    Empregado empregado = Empregado.builder()
        .matriculaEmpregado(empregadoInputDto.getMatriculaEmpregado())
        .nomeEmpregado(empregadoInputDto.getNomeEmpregado())
      .build();
    empregados.add(empregado);
    return new EmpregadoOutputDto(empregado);
  }

}
