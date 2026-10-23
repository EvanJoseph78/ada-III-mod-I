package br.com.ada.selecaointerna.empregadosservice.interfaces.controller;

import br.com.ada.selecaointerna.empregadosservice.interfaces.dto.EmpregadoInputDto;
import br.com.ada.selecaointerna.empregadosservice.interfaces.dto.EmpregadoOutputDto;
import br.com.ada.selecaointerna.empregadosservice.service.EmpregadosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/empregados")
@RequiredArgsConstructor
public class EmpregadosController {

  private final EmpregadosService service;

  @GetMapping
  public ResponseEntity<List<EmpregadoOutputDto>> obterTodosEmpregados() {
    return ResponseEntity.status(200).body(service.obterTodosEmpregados());

  }

  @GetMapping("/{matriculaEmpregado}")
  public ResponseEntity<EmpregadoOutputDto> obterEmpregado(@PathVariable("matriculaEmpregado") Long matriculaEmpregado) {
    return ResponseEntity.status(200).body(service.obterEmpregado(matriculaEmpregado));
  }

  @PostMapping
  public ResponseEntity<EmpregadoOutputDto> cadastrarEmpregado(@RequestBody EmpregadoInputDto empregadoInputDto) {
    return ResponseEntity.status(201).body(service.cadastrarEmpregado(empregadoInputDto));
  }

  @GetMapping("/{matriculaEmpregado}/curriculo-profissional")
  public ResponseEntity<EmpregadoOutputDto> obterCurriculoProfissional(@PathVariable("matriculaEmpregado") Long matriculaEmpregado) {
    return ResponseEntity.status(200).body(service.obterEmpregado(matriculaEmpregado));
  }

  @GetMapping("/{matriculaEmpregado}/curriculo-educacional")
  public ResponseEntity<EmpregadoOutputDto> obterCurriculoEducacional(@PathVariable("matriculaEmpregado") Long matriculaEmpregado) {
    return ResponseEntity.status(200).body(service.obterEmpregado(matriculaEmpregado));
  }

}
