package org.acme.endpoint;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.acme.domain.Pessoa;
import org.acme.domain.port.CountPessoas;
import org.acme.domain.port.CreatePessoa;
import org.acme.domain.port.GetPessoaById;
import org.acme.domain.port.ListPessoasByText;

import java.net.URI;
import java.util.Collections;
import java.util.List;
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
                .onItem().transform(this::buildCreatedResponse)
                .onFailure().recoverWithItem(this::buildErrorResponse);
    }

    @GET
    @Path("pessoas/{id}")
    public Uni<Response> getSingle(UUID id) {
        return getById.byId(id)
                .onItem().transform(pessoa -> pessoa != null ?
                        Response.ok(pessoa).build() :
                        buildNotFoundResponse(id)
                );
    }

    @GET
    @Path("pessoas")
    public Multi<Pessoa> getPessoa(@QueryParam("t") String text) {
        if (text == null || text.isEmpty()) {
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
        }

        return listByText.byText(text);
    }

    @GET
    @Path("contagem-pessoas")
    public Uni<Long> countPessoas() {
        return countPessoas.doCount();
    }

    private Response buildCreatedResponse(Pessoa pessoa) {
        return Response.created(URI.create("/pessoas/%s".formatted(pessoa.id())))
                .entity(pessoa).build();
    }

    private Response buildErrorResponse(Throwable ex) {
        return Response.status(422).entity(new ErrorMessages(
                Collections.singletonList(ex.getMessage())
        )).build();
    }

    private Response buildNotFoundResponse(UUID uuid) {
        return Response.status(404).entity(new ErrorMessages(
                Collections.singletonList("Pessoa com id %s não encontrada".formatted(uuid.toString())))
        ).build();
    }
}