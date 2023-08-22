package org.acme.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.inject.Singleton;
import org.acme.domain.Pessoa;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class CacheAdapter {

    final ObjectMapper objectMapper = new ObjectMapper();

    final Cache<Object, Object> pessoasCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(10))
            .maximumSize(1000)
            .build();

    public Pessoa get(UUID uuid) {
        return (Pessoa) pessoasCache.getIfPresent(uuid.toString());
    }

    public Object put(String event) {
        try {
            JsonNode jsonNode = objectMapper.readTree(event);
            return this.put(new Pessoa(
                    jsonNode.get("id").asText(),
                    jsonNode.get("apelido").asText(),
                    jsonNode.get("nome").asText(),
                    LocalDate.parse(jsonNode.get("nascimento").asText()),
                    Optional.ofNullable(jsonNode.get("apelido").asText()).map(text -> List.of(text.split(","))).orElse(null)

            ));
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Object put(Pessoa pessoa) {
        pessoasCache.put(pessoa.id(), pessoa);
        return pessoa.id();
    }

}
