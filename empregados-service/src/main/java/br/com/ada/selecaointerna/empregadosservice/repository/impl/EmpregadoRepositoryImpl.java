package br.com.ada.selecaointerna.empregadosservice.repository.impl;

import br.com.ada.selecaointerna.empregadosservice.entity.domain.Empregado;
import br.com.ada.selecaointerna.empregadosservice.entity.infra.EmpregadoEntity;
import br.com.ada.selecaointerna.empregadosservice.repository.EmpregadoRepository;
import br.com.ada.selecaointerna.empregadosservice.repository.jpa.EmpregadoRepositoryJPA;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EmpregadoRepositoryImpl implements EmpregadoRepository {

    @Autowired
    EmpregadoRepositoryJPA jpaRepository;

    @Override
    public void salvarEmpregado(Empregado empregado) {
        EmpregadoEntity entity = toEntity(empregado);
        jpaRepository.save(entity);
    }

    @Override
    public Empregado buscarPorMatricula(Long matricula) {
        Optional<EmpregadoEntity> empregadoEntity = jpaRepository.findByMatricula(matricula);
        if(empregadoEntity.isEmpty()){
            throw new RuntimeException("Não encontrado empregado com essa matricula.");
        }
        return toDomain(empregadoEntity.get());
    }

    @Override
    public List<Empregado> buscarTodos() {
        List<EmpregadoEntity> entityList = jpaRepository.findAll();
        List<Empregado> empregados = new ArrayList<>();
        for(EmpregadoEntity entity : entityList){
            empregados.add(toDomain(entity));
        }
        return empregados;
    }

    EmpregadoEntity toEntity(Empregado empregado){
        return new EmpregadoEntity(
                empregado.getMatricula(),
                empregado.getNome()
        );
    }

    Empregado toDomain(EmpregadoEntity entity){
        return new Empregado(
                entity.getMatricula(),
                entity.getNome()
        );
    }
}
