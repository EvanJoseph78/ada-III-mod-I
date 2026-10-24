package br.com.ada.selecaointerna.candidaturasservice;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RankingService {
  private static final Logger log = LoggerFactory.getLogger(RankingService.class);
  private final AvaliacaoRepository avaliacoes;
  private final RankingConsulta consulta;
  private final CacheManager cacheManager;

  public RankingDto obter(UUID vagaId) {
    // Avaliacoes sao imutaveis e somente inseridas: cada commit cria uma nova revisao.
    long revisao = avaliacoes.countByVagaId(vagaId);
    String chave = vagaId + ":" + revisao;
    Cache cache = null;
    try {
      cache = cacheManager.getCache(RankingCacheConfig.CACHE);
      if (cache != null) {
        RankingDto valor = cache.get(chave, RankingDto.class);
        if (valor != null && vagaId.equals(valor.vagaId())) return valor;
      }
    } catch (RuntimeException e) {
      log.warn("Cache de ranking indisponivel na leitura ({}); consultando banco", e.getClass().getSimpleName());
      cache = null;
    }
    // Nao capturar falhas do banco como se fossem falhas do cache.
    RankingDto ranking = consulta.consultar(vagaId);
    if (cache != null) {
      try { cache.put(chave, ranking); }
      catch (RuntimeException e) {
        log.warn("Cache de ranking indisponivel na escrita ({}); resposta preservada", e.getClass().getSimpleName());
      }
    }
    return ranking;
  }
}
