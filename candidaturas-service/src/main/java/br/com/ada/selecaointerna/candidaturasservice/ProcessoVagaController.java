package br.com.ada.selecaointerna.candidaturasservice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/candidaturas/processos")
public class ProcessoVagaController {
  private final ProcessoVagaService service;
  @PostMapping("/{vagaId}/encerramento")
  public ProcessoVagaService.Prontidao encerrar(@PathVariable UUID vagaId, @Valid @RequestBody Encerramento input) {
    return service.encerrar(vagaId, input.idVaga());
  }
  @GetMapping("/{vagaId}/prontidao")
  public ProcessoVagaService.Prontidao prontidao(@PathVariable UUID vagaId) { return service.consultar(vagaId); }
  public record Encerramento(@NotNull @Positive Long idVaga) {}
}
