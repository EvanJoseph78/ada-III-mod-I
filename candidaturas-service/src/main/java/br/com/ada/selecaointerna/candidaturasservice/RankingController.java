package br.com.ada.selecaointerna.candidaturasservice;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/candidaturas/ranking")
public class RankingController {
  private final RankingConsulta consulta;

  @GetMapping("/{vagaId}")
  public RankingDto obter(@PathVariable UUID vagaId) { return consulta.consultar(vagaId); }
}
