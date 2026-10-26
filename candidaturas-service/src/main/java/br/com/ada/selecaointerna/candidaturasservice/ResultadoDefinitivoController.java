package br.com.ada.selecaointerna.candidaturasservice;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/candidaturas/resultados")
public class ResultadoDefinitivoController {
  private final ResultadoDefinitivoService service;
  @GetMapping("/{vagaId}")
  public ResultadoDefinitivoDto consultar(@PathVariable UUID vagaId) { return service.consultar(vagaId); }
}
