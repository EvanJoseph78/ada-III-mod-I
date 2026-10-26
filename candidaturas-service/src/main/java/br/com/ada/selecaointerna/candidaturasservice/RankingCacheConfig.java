package br.com.ada.selecaointerna.candidaturasservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;

@Configuration
public class RankingCacheConfig {
  public static final String CACHE = "ranking";
  public static final String PREFIXO = "selecao:ranking:v1::";

  @Bean
  CacheManager rankingCacheManager(ObjectMapper mapper, ObjectProvider<RedisConnectionFactory> connections,
      ObjectProvider<Ticker> tickers, @Value("${ranking.cache.tipo:redis}") String tipo,
      @Value("${ranking.cache.ttl-segundos:60}") long segundos) {
    if (segundos <= 0) throw new IllegalArgumentException("TTL do ranking deve ser positivo");
    Duration ttl = Duration.ofSeconds(segundos);
    if ("caffeine".equals(tipo)) {
      CaffeineCacheManager manager = new CaffeineCacheManager(CACHE);
      manager.setAllowNullValues(false);
      manager.setCaffeine(Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(1000)
          .ticker(tickers.getIfAvailable(Ticker::systemTicker)));
      return manager;
    }
    if (!"redis".equals(tipo)) throw new IllegalArgumentException("Tipo de cache desconhecido: " + tipo);
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(ttl).disableCachingNullValues().computePrefixWith(name -> PREFIXO)
        .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer(mapper)));
    return RedisCacheManager.builder(connections.getObject()).cacheDefaults(config)
        .initialCacheNames(Set.of(CACHE)).build();
  }

  static Jackson2JsonRedisSerializer<RankingDto> serializer(ObjectMapper mapper) {
    return new Jackson2JsonRedisSerializer<>(mapper, RankingDto.class);
  }
}
