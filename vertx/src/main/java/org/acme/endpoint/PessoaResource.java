package org.acme.endpoint;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.domain.Pessoa;
import org.acme.domain.SearchInvalidError;
import org.acme.domain.port.CountPessoas;
import org.acme.domain.port.CreatePessoa;
import org.acme.domain.port.GetPessoaById;
import org.acme.domain.port.ListPessoasByText;

import java.net.URI;
import java.util.UUID;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PessoaResource {

    @Inject CreatePessoa createNew;
    @Inject GetPessoaById getById;
    @Inject ListPessoasByText listByText;
    @Inject CountPessoas countPessoas;

    @POST
    @Path("pessoas")
    public Uni<Response> createPessoa(Pessoa pessoa) {
        return createNew.create(pessoa)
                .onItem().transform(newPessoa ->
                        Response.created(URI.create("/pessoas/%s".formatted(newPessoa.id())))
                                .entity(newPessoa).build());
    }

    @GET
    @Path("pessoas/{id}")
    public Uni<Pessoa> getSingle(UUID id) {
        return getById.byId(id);
    }

    @GET
    @Path("pessoas")
    public Multi<Pessoa> getPessoa(@QueryParam("t") String text) {
        if (text == null || text.isEmpty()) throw new SearchInvalidError();
        return listByText.byText(text);
    }

    @GET
    @Path("contagem-pessoas")
    public Uni<Long> countPessoas() {
        return countPessoas.doCount();
    }
}