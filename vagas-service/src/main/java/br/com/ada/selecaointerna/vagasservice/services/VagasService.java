package br.com.ada.selecaointerna.vagasservice.services;

import br.com.ada.selecaointerna.vagasservice.dtos.VagaInputDto;
import br.com.ada.selecaointerna.vagasservice.dtos.VagaOutputDto;
import br.com.ada.selecaointerna.vagasservice.models.Vaga;
import br.com.ada.selecaointerna.vagasservice.repositories.VagasRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VagasService {

  private final VagasRepository vagasRepository;

  public List<VagaOutputDto> obterTodasVagas() {
    return vagasRepository.findAll().stream().map(VagaOutputDto::new).toList();
  }

  public VagaOutputDto obterVaga(UUID idVaga) {
    return new VagaOutputDto(vagasRepository.findById(idVaga)
        .orElseThrow(EntityNotFoundException::new));
  }

  @Transactional
  public VagaOutputDto cadastrarVaga(VagaInputDto vagaInputDto) {
    Vaga vaga = Vaga.builder()
        .idVaga(UUID.randomUUID())
        .nomeVaga(vagaInputDto.getNomeVaga())
      .build();
    return new VagaOutputDto(vagasRepository.save(vaga));
  }

  @Transactional
  public void removerVaga(UUID idVaga) {
    vagasRepository.deleteById(idVaga);
  }

}
