package br.com.ada.selecaointerna.candidaturasservice;

import br.com.ada.selecaointerna.contracts.*;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class AvaliadorCurricular {
  public Resultado avaliar(PerfilProfissionalDto perfil, CriteriosVagaDto criterios) {
    int pontos = 0;
    boolean elegivel = true;
    for (RequisitoDto requisito : criterios.requisitos()) {
      String palavra = requisito.palavraChave().strip().toLowerCase(Locale.ROOT);
      boolean atende = perfil.itens().stream().anyMatch(item -> item.tipo() == requisito.tipo()
          && item.descricao().toLowerCase(Locale.ROOT).contains(palavra)
          && (requisito.mesesMinimos() == null || requisito.mesesMinimos() == 0
              || (item.meses() != null && item.meses() >= requisito.mesesMinimos())));
      if (atende) {
        try { pontos = Math.addExact(pontos, requisito.pontos()); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Pontuacao excede limite inteiro", e); }
      } else if (requisito.obrigatorio()) { elegivel = false; }
    }
    return new Resultado(elegivel, elegivel ? pontos : 0);
  }

  public record Resultado(boolean elegivel, int pontuacao) {}
}
