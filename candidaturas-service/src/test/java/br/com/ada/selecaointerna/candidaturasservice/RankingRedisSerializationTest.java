package br.com.ada.selecaointerna.candidaturasservice;

import com.fasterxml.jackson.databind.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.SerializationException;
import static org.assertj.core.api.Assertions.*;

class RankingRedisSerializationTest {
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @Test
  void jsonTipadoPreservaUuidDataListaEPosicoesSemMetadadosDeClasseJava() {
    var dto = new RankingDto(UUID.randomUUID(), List.of(new RankingDto.Classificado(
        1, UUID.randomUUID(), UUID.randomUUID(), 123L, 10, Instant.parse("2026-10-24T12:00:00.123456Z"))));
    var serializer = RankingCacheConfig.serializer(mapper);
    byte[] bytes = serializer.serialize(dto);
    assertThat(new String(bytes, StandardCharsets.UTF_8)).contains("classificados", "2026-10-24T12:00:00.123456Z")
        .doesNotContain("@class", "br.com.ada", "eventoCandidatura", "perfilSnapshot");
    assertThat(serializer.deserialize(bytes)).isEqualTo(dto);
  }

  @Test
  void jsonCorrompidoProduzErroDeCacheTratavelPeloFallback() {
    assertThatThrownBy(() -> RankingCacheConfig.serializer(mapper).deserialize("{corrompido".getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(SerializationException.class);
  }
}
