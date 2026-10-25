package br.com.ada.selecaointerna.vagasservice;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import br.com.ada.selecaointerna.contracts.CriteriosVagaDto;

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
  public ResponseEntity<VagaOutputDto> cadastrarVaga(@Valid @RequestBody VagaInputDto vagaInputDto) {
    return ResponseEntity.status(201).body(service.cadastrarVaga(vagaInputDto));
  }

  @DeleteMapping("/{idVaga}")
  public ResponseEntity<Void> removerVaga(@PathVariable("idVaga") Long idVaga) {
    service.removerVaga(idVaga);
    return ResponseEntity.status(204).build();
  }

  @GetMapping("/{id}/criterios")
  public CriteriosVagaDto obterCriterios(@PathVariable UUID id) {
    return service.obterCriterios(id);
  }

  @PostMapping("/{id}/encerramento")
  public VagaOutputDto encerrarInscricoes(@PathVariable UUID id) { return service.encerrarInscricoes(id); }

}
