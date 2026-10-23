package br.com.ada.selecaointerna.empregadosservice.repository;
import br.com.ada.selecaointerna.empregadosservice.entity.domain.Empregado;
import java.util.List;

public interface EmpregadoRepository {
    void salvarEmpregado(Empregado empregado);
    Empregado buscarPorMatricula(Long matricula);
    List<Empregado> buscarTodos();
}
