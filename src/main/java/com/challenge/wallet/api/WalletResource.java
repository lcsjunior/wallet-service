package com.challenge.wallet.api;

import com.challenge.wallet.service.WalletService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;

import java.util.UUID;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.CREATED;

@Path("/wallet")
public class WalletResource {

    @Inject
    WalletService walletService;

    @POST
    @Produces(APPLICATION_JSON)
    @Operation(summary = "Create a new wallet", description = "Creates a wallet and returns the wallet ID.")
    public Response createWallet() {
        return Response.status(CREATED).entity(walletService.createWallet()).build();
    }

    @GET
    @Path("/{id}/balance")
    @Produces(APPLICATION_JSON)
    @Operation(summary = "Get the balance of a wallet", description = "Retrieves the balance of a specific wallet by ID.")
    public Response getBalance(@PathParam("id") UUID walletId) {
        return Response.ok(walletService.getWallet(walletId)).build();
    }
}
