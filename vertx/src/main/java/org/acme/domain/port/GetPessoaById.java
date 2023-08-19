package org.acme.domain.port;

import io.smallrye.mutiny.Uni;
import org.acme.domain.Pessoa;

import java.util.UUID;

public interface GetPessoaById {

    Uni<Pessoa> byId(UUID id);

}
