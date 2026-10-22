package br.com.ada.selecaointerna.vagasservice;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VagasService {

  private final List<Vaga> vagas = new ArrayList<>();

  public List<VagaOutputDto> obterTodasVagas() {
    return vagas.stream().map(VagaOutputDto::new).toList();
  }

  public VagaOutputDto obterVaga(Long idVaga) {
    return new VagaOutputDto(vagas.stream().filter(vaga -> vaga.getIdVaga().equals(idVaga)).findFirst().get());
  }

  public VagaOutputDto cadastrarVaga(VagaInputDto vagaInputDto) {
    Vaga vaga = Vaga.builder()
        .idVaga(vagas.size() + 1L)
        .nomeVaga(vagaInputDto.getNomeVaga())
      .build();
    vagas.add(vaga);
    return new VagaOutputDto(vaga);
  }

  public void removerVaga(Long idVaga) {
    vagas.remove(idVaga.intValue());
  }

}
