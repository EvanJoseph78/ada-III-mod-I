package br.com.ada.selecaointerna.empregadosservice;

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

}
