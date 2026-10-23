package br.com.ada.selecaointerna.candidaturasservice.controllers;

import br.com.ada.selecaointerna.candidaturasservice.dtos.CandidaturaInputDto;
import br.com.ada.selecaointerna.candidaturasservice.dtos.CandidaturaOutputDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/candidaturas")
@RequiredArgsConstructor
public class CandidaturasController {

  @PostMapping
  public ResponseEntity<CandidaturaOutputDto> realizarCandidatura(@RequestBody CandidaturaInputDto candidaturaInputDto) {
    return ResponseEntity.status(202).body(new CandidaturaOutputDto());
  }

}
