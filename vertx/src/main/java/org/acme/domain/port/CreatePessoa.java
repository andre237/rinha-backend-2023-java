package org.acme.domain.port;

import io.smallrye.mutiny.Uni;
import org.acme.domain.Pessoa;

public interface CreatePessoa {

    Uni<Pessoa> create(Pessoa pessoa);

}
