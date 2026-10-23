package br.com.ada.selecaointerna.candidaturasservice;

import java.util.UUID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/candidaturas")
@RequiredArgsConstructor
public class CandidaturasController {
  private final CandidaturasService service;

  @PostMapping
  public ResponseEntity<CandidaturaOutputDto> realizarCandidatura(@Valid @RequestBody CandidaturaInputDto input) {
    return ResponseEntity.status(201).body(service.realizarCandidatura(input));
  }

  @GetMapping("/{id}")
  public CandidaturaOutputDto obterCandidatura(@PathVariable UUID id) {
    return service.obterCandidatura(id);
  }
}
