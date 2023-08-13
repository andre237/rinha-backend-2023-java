package org.acme.domain.port;

import io.smallrye.mutiny.Uni;

public interface CountPessoas {

    Uni<Long> doCount();

}
