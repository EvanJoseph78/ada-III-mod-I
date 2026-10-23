package br.com.ada.selecaointerna.empregadosservice.controllers;

import br.com.ada.selecaointerna.empregadosservice.dtos.EmpregadoInputDto;
import br.com.ada.selecaointerna.empregadosservice.dtos.EmpregadoOutputDto;
import br.com.ada.selecaointerna.empregadosservice.services.EmpregadosService;
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
  public ResponseEntity<List<EmpregadoOutputDto>> obterTodosEmpregados() {
    return ResponseEntity.status(200).body(service.obterTodosEmpregados());

  }

  @GetMapping("/{idEmpregado}")
  public ResponseEntity<EmpregadoOutputDto> obterEmpregado(@PathVariable("idEmpregado") UUID idEmpregado) {
    return ResponseEntity.status(200).body(service.obterEmpregado(idEmpregado));
  }

  @PostMapping
  public ResponseEntity<EmpregadoOutputDto> cadastrarEmpregado(@RequestBody EmpregadoInputDto empregadoInputDto) {
    return ResponseEntity.status(201).body(service.cadastrarEmpregado(empregadoInputDto));
  }

}
