package br.com.ada.selecaointerna.vagasservice;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
  public ResponseEntity<VagaOutputDto> obterVaga(@PathVariable("idVaga") Long idVaga) {
    return ResponseEntity.status(200).body(service.obterVaga(idVaga));
  }

  @PostMapping
  public ResponseEntity<VagaOutputDto> cadastrarVaga(@RequestBody VagaInputDto vagaInputDto) {
    return ResponseEntity.status(201).body(service.cadastrarVaga(vagaInputDto));
  }

  @DeleteMapping("/{idVaga}")
  public ResponseEntity<Void> removerVaga(@RequestParam("idVaga") Long idVaga) {
    service.removerVaga(idVaga);
    return ResponseEntity.status(204).build();
  }

}
