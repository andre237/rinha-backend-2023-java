package org.acme.adapter;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import io.vertx.mutiny.sqlclient.Tuple;

import org.acme.domain.Pessoa;
import org.acme.domain.port.CountPessoas;
import org.acme.domain.port.CreatePessoa;
import org.acme.domain.port.GetPessoaById;
import org.acme.domain.port.ListPessoasByText;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class PgQueriesAdapter implements ListPessoasByText, GetPessoaById, CreatePessoa, CountPessoas {

    @Inject
    io.vertx.mutiny.pgclient.PgPool pgClient;

    @Override
    public Uni<Pessoa> create(Pessoa pessoa) {
        return pgClient.preparedQuery("insert into public.pessoa(apelido, nome, nascimento, stack) VALUES ($1, $2, $3, $4) returning id")
                .execute(Tuple.of(pessoa.apelido(), pessoa.nome(), pessoa.nascimento(), String.join(",", Optional.ofNullable(pessoa.stack()).orElse(Collections.emptyList()))))
                .onItem().transform(rs -> {
                    UUID id = rs.iterator().next().getUUID("id");
                    return new Pessoa(id.toString(), pessoa.apelido(), pessoa.nome(), pessoa.nascimento(), pessoa.stack());
                });
    }

    @Override
    public Uni<Pessoa> byId(UUID id) {
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
