package br.com.ada.selecaointerna.vagasservice.controllers;

import br.com.ada.selecaointerna.vagasservice.dtos.VagaInputDto;
import br.com.ada.selecaointerna.vagasservice.dtos.VagaOutputDto;
import br.com.ada.selecaointerna.vagasservice.services.VagasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vagas")
@RequiredArgsConstructor
public class VagasController {

  private final VagasService service;

  @GetMapping
  public ResponseEntity<List<VagaOutputDto>> obterTodasVagas() {
    return ResponseEntity.status(200).body(service.obterTodasVagas());

  }

  @GetMapping("/{idVaga}")
  public ResponseEntity<VagaOutputDto> obterVaga(@PathVariable("idVaga") UUID idVaga) {
    return ResponseEntity.status(200).body(service.obterVaga(idVaga));
  }

  @PostMapping
  public ResponseEntity<VagaOutputDto> cadastrarVaga(@RequestBody VagaInputDto vagaInputDto) {
    return ResponseEntity.status(201).body(service.cadastrarVaga(vagaInputDto));
  }

  @DeleteMapping("/{idVaga}")
  public ResponseEntity<Void> removerVaga(@RequestParam("idVaga") UUID idVaga) {
    service.removerVaga(idVaga);
    return ResponseEntity.status(204).build();
  }

}
