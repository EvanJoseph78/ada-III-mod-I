package br.com.ada.selecaointerna.empregadosservice.services;

import br.com.ada.selecaointerna.empregadosservice.dtos.EmpregadoInputDto;
import br.com.ada.selecaointerna.empregadosservice.dtos.EmpregadoOutputDto;
import br.com.ada.selecaointerna.empregadosservice.models.Empregado;
import br.com.ada.selecaointerna.empregadosservice.repositories.EmpregadosRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmpregadosService {

  private final EmpregadosRepository empregadosRepository;

  public List<EmpregadoOutputDto> obterTodosEmpregados() {
    return empregadosRepository.findAll().stream().map(EmpregadoOutputDto::new).toList();
  }

  public EmpregadoOutputDto obterEmpregado(UUID idEmpregado) {
    return new EmpregadoOutputDto(empregadosRepository.findById(idEmpregado)
        .orElseThrow(EntityNotFoundException::new));
  }

  @Transactional
  public EmpregadoOutputDto cadastrarEmpregado(EmpregadoInputDto empregadoInputDto) {
    Empregado empregado = Empregado.builder()
        .idEmpregado(UUID.randomUUID())
        .matriculaEmpregado(empregadoInputDto.getMatriculaEmpregado())
        .nomeEmpregado(empregadoInputDto.getNomeEmpregado())
      .build();
    return new EmpregadoOutputDto(empregadosRepository.save(empregado));
  }

}
