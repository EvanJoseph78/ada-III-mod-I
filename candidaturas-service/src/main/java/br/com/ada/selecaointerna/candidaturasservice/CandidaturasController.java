package br.com.ada.selecaointerna.candidaturasservice;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/candidaturas")
@RequiredArgsConstructor
public class CandidaturasController {

  @PostMapping
  public ResponseEntity<CandidaturaOutputDto> realizarCandidatura(@RequestBody CandidaturaInputDto candidaturaInputDto) {
    return ResponseEntity.status(201).body(new CandidaturaOutputDto());
  }

}
