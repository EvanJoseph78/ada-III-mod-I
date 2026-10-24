package br.com.ada.selecaointerna.empregadosservice.interfaces.controller;

import br.com.ada.selecaointerna.contracts.PerfilProfissionalDto;
import br.com.ada.selecaointerna.empregadosservice.interfaces.dto.*;
import br.com.ada.selecaointerna.empregadosservice.service.EmpregadosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/empregados")
@RequiredArgsConstructor
public class EmpregadosController {
  private final EmpregadosService service;

  @GetMapping
  public List<EmpregadoOutputDto> obterTodosEmpregados() { return service.obterTodosEmpregados(); }

  @GetMapping("/{matriculaEmpregado}")
  public EmpregadoOutputDto obterEmpregado(@PathVariable Long matriculaEmpregado) {
    return service.obterEmpregado(matriculaEmpregado);
  }

  @PostMapping
  public ResponseEntity<EmpregadoOutputDto> cadastrarEmpregado(@Valid @RequestBody EmpregadoInputDto input) {
    return ResponseEntity.status(201).body(service.cadastrarEmpregado(input));
  }

  @GetMapping("/{id}/perfil")
  public PerfilProfissionalDto obterPerfil(@PathVariable UUID id) { return service.obterPerfil(id); }

  @GetMapping("/{matriculaEmpregado}/curriculo-profissional")
  public EmpregadoOutputDto obterCurriculoProfissional(@PathVariable Long matriculaEmpregado) {
    return service.obterCurriculo(matriculaEmpregado, true);
  }

  @GetMapping("/{matriculaEmpregado}/curriculo-educacional")
  public EmpregadoOutputDto obterCurriculoEducacional(@PathVariable Long matriculaEmpregado) {
    return service.obterCurriculo(matriculaEmpregado, false);
  }
}
