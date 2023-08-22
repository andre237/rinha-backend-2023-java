package org.acme.adapter;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.pgclient.pubsub.PgSubscriber;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;

import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.PoolOptions;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.acme.domain.Pessoa;
import org.acme.domain.port.CountPessoas;
import org.acme.domain.port.CreatePessoa;
import org.acme.domain.port.GetPessoaById;
import org.acme.domain.port.ListPessoasByText;

import jakarta.inject.Singleton;
import org.jboss.logging.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class PgQueriesAdapter implements ListPessoasByText, GetPessoaById, CreatePessoa, CountPessoas {

    final CacheAdapter cacheAdapter;
    final org.jboss.logging.Logger logger;
    final io.vertx.mutiny.pgclient.PgPool pgClient;
    final io.vertx.pgclient.PgConnectOptions connectOptions;

    @Inject
    public PgQueriesAdapter(CacheAdapter cacheAdapter, Logger logger, PgConnectOptions connectOptions) {
        this.cacheAdapter = cacheAdapter;
        this.logger = logger;
        this.connectOptions = connectOptions;
        this.pgClient = PgPool.pool(Vertx.vertx(), connectOptions, new PoolOptions().setMaxSize(5));
    }

    void init(@Observes StartupEvent event) {
        PgSubscriber subscriber = PgSubscriber.subscriber(Vertx.vertx(), connectOptions);
        subscriber.channel("cacheChannel").handler(cacheAdapter::put);
        subscriber.connectAndAwait();

        logger.infov("Started cache channel subscriber");
    }


    @Override
    public Uni<Pessoa> create(Pessoa pessoa) {
        return pgClient.preparedQuery("insert into public.pessoa(apelido, nome, nascimento, stack) VALUES ($1, $2, $3, $4) returning id")
                .execute(Tuple.of(pessoa.apelido(), pessoa.nome(), pessoa.nascimento(), String.join(",", Optional.ofNullable(pessoa.stack()).orElse(Collections.emptyList()))))
                .onItem().transform(rs -> {
                    UUID id = rs.iterator().next().getUUID("id");
                    Pessoa saved = new Pessoa(id.toString(), pessoa.apelido(), pessoa.nome(), pessoa.nascimento(), pessoa.stack());
                    cacheAdapter.put(saved);
                    return saved;
                });
    }

    @Override
    public Uni<Pessoa> byId(UUID id) {
        Pessoa cached = cacheAdapter.get(id);
        if (cached != null) {
            logger.infov("Found on cache");
            return Uni.createFrom().item(cached);
        }

        return pgClient.preparedQuery("select * from public.pessoa where id = $1").execute(Tuple.of(id))
                 .onItem().transform(RowSet::iterator)
                 .onItem().transform(iterator -> iterator.hasNext() ? fromRow(iterator.next()) : null);
    }

    @Override
    public Multi<Pessoa> byText(String searchText) {
        return pgClient.preparedQuery("select * from public.pessoa where " +
                        "to_tsvector('simple', apelido || ' ' || nome || ' ' || stack) @@ plainto_tsquery($1) limit 50")
                .execute(Tuple.of(searchText))
                .onItem().transformToMulti(rs -> Multi.createFrom().iterable(rs))
                .onItem().transform(this::fromRow);
    }

    @Override
    public Uni<Long> doCount() {
        return pgClient.preparedQuery("select count(*) from public.pessoa")
                .execute()
                .onItem().transform(RowSet::iterator)
                .onItem().transform(iterator -> iterator.next().getLong("count"));
    }

    private Pessoa fromRow(Row row) {
        return new Pessoa(
                row.getUUID("id").toString(),
                row.getString("apelido"),
                row.getString("nome"),
                row.getLocalDate("nascimento"),
                Optional.ofNullable(row.getString("stack")).map(text -> List.of(text.split(","))).orElse(null)
        );
    }

}
