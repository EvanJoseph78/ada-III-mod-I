package br.com.ada.selecaointerna.candidaturasservice;

import br.com.ada.selecaointerna.contracts.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AvaliadorCurricularTest {
  private final AvaliadorCurricular avaliador = new AvaliadorCurricular();

  @Test
  void somaUmaVezPorRequisitoMesmoComMultiplosItensSemDiferenciarMaiusculas() {
    var resultado = avaliar(List.of(new ItemPerfilDto(TipoItem.CURSO, "Java basico", 40, null),
        new ItemPerfilDto(TipoItem.CURSO, "JAVA avancado", 40, null)),
        List.of(new RequisitoDto(TipoItem.CURSO, " JAVA ", true, 10, null)));
    assertThat(resultado).isEqualTo(new AvaliadorCurricular.Resultado(true, 10));
  }

  @Test
  void obrigatorioNaoAtendidoZeraPontuacaoMesmoComOpcionalAtendido() {
    var resultado = avaliar(List.of(new ItemPerfilDto(TipoItem.CURSO, "Java", 40, null)), List.of(
        new RequisitoDto(TipoItem.CURSO, "Java", false, 10, null),
        new RequisitoDto(TipoItem.CERTIFICACAO, "Java", true, 20, null)));
    assertThat(resultado).isEqualTo(new AvaliadorCurricular.Resultado(false, 0));
  }

  @Test
  void exigeMesesMinimosNoMesmoItemCorrespondente() {
    var requisito = List.of(new RequisitoDto(TipoItem.EXPERIENCIA_INTERNA, "Java", true, 10, 12));
    assertThat(avaliar(List.of(new ItemPerfilDto(TipoItem.EXPERIENCIA_INTERNA, "Java", null, 11)), requisito).elegivel()).isFalse();
    assertThat(avaliar(List.of(new ItemPerfilDto(TipoItem.EXPERIENCIA_INTERNA, "Java", null, 12)), requisito).pontuacao()).isEqualTo(10);
  }

  @Test
  void opcionalAusenteNaoDesclassificaEPerfilSemRequisitosRecebeZero() {
    assertThat(avaliar(List.of(), List.of(new RequisitoDto(TipoItem.CURSO, "Java", false, 10, null))))
        .isEqualTo(new AvaliadorCurricular.Resultado(true, 0));
    assertThat(avaliar(List.of(), List.of())).isEqualTo(new AvaliadorCurricular.Resultado(true, 0));
  }

  @Test
  void rejeitaSomaQueExcedeLimiteInteiroEmVezDePersistirPontuacaoNegativa() {
    assertThatThrownBy(() -> avaliar(List.of(new ItemPerfilDto(TipoItem.CURSO, "Java", 40, null)), List.of(
        new RequisitoDto(TipoItem.CURSO, "Java", false, Integer.MAX_VALUE, null),
        new RequisitoDto(TipoItem.CURSO, "Java", false, 1, null))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private AvaliadorCurricular.Resultado avaliar(List<ItemPerfilDto> itens, List<RequisitoDto> requisitos) {
    return avaliador.avaliar(new PerfilProfissionalDto(UUID.randomUUID(), "Ana", itens),
        new CriteriosVagaDto(UUID.randomUUID(), "Java", StatusVaga.ABERTA, requisitos));
  }
}
