package br.com.ada.selecaointerna.candidaturasservice;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @RequiredArgsConstructor
public class ProcessoVagaService {
  static final UUID BARREIRA = new UUID(0, 0);
  private final ProcessoVagaRepository processos;
  private final CandidaturaRepository candidaturas;
  private final AvaliacaoRepository avaliacoes;

  @Transactional
  public void validarInscricao(UUID vagaId, Long idVaga) {
    processos.bloquear(BARREIRA).orElseThrow();
    if (processos.existsById(vagaId) || processos.existsByIdVaga(idVaga)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Inscricoes encerradas para a vaga");
    }
  }

  @Transactional
  public Prontidao encerrar(UUID vagaId, Long idVaga) {
    processos.bloquear(BARREIRA).orElseThrow();
    ProcessoVaga processo = processos.findById(vagaId).orElseGet(() -> {
      if (processos.existsByIdVaga(idVaga)) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Identificadores da vaga divergentes");
      }
      return processos.saveAndFlush(new ProcessoVaga(vagaId, idVaga));
    });
    if (!processo.getIdVaga().equals(idVaga)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Identificadores da vaga divergentes");
    }
    return prontidao(processo);
  }

  @Transactional(readOnly = true)
  public Prontidao consultar(UUID vagaId) {
    return prontidao(processos.findById(vagaId)
        .filter(p -> !p.getVagaId().equals(BARREIRA))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inscricoes ainda nao encerradas")));
  }

  private Prontidao prontidao(ProcessoVaga processo) {
    long inscricoes = candidaturas.countByIdVaga(processo.getIdVaga());
    long avaliadas = avaliacoes.countByVagaId(processo.getVagaId());
    return new Prontidao(processo.getVagaId(), processo.getIdVaga(), inscricoes, avaliadas, inscricoes == avaliadas);
  }

  public record Prontidao(UUID vagaId, Long idVaga, long inscricoes, long avaliacoes, boolean apto) {}
}
