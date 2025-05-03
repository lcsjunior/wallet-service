package com.challenge.wallet.api;

import com.challenge.wallet.dto.DepositRequest;
import com.challenge.wallet.dto.HistoricalBalanceQuery;
import com.challenge.wallet.dto.TransferRequest;
import com.challenge.wallet.dto.WithdrawRequest;
import com.challenge.wallet.service.WalletService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.CREATED;

@Path("/wallets")
@Tag(name = "Wallet Service Assignment")
public class WalletResource {

    @Inject
    WalletService walletService;

    @POST
    @Produces(APPLICATION_JSON)
    @Operation(summary = " Allow the creation of wallets for users")
    public Response createWallet() {
        return Response.status(CREATED).entity(walletService.createWallet()).build();
    }

    @GET
    @Path("/{id}/balance")
    @Produces(APPLICATION_JSON)
    @Operation(summary = "Retrieve the current balance of a wallet")
    public Response getBalance(@PathParam("id") UUID walletId) {
        return Response.ok(walletService.getBalance(walletId)).build();
    }

    @GET
    @Path("/{id}/historical-balance")
    @Produces(APPLICATION_JSON)
    @Operation(summary = "Retrieve the balance of a wallet at a specific point in the past")
    public Response getHistoricalBalance(@PathParam("id") UUID walletId,
                                         @Valid @BeanParam HistoricalBalanceQuery query) {
        return Response.ok(walletService.getHistoricalBalance(walletId, query)).build();
    }

    @POST
    @Path("/deposit")
    @Produces(APPLICATION_JSON)
    @Operation(summary = "Enable users to deposit money into their wallets")
    public Response deposit(@Valid DepositRequest depositRequest) {
        walletService.deposit(depositRequest);
        return Response.ok().build();
    }

    @POST
    @Path("/withdraw")
    @Produces(APPLICATION_JSON)
    @Operation(summary = "Enable users to withdraw money from their wallets")
    public Response withdraw(@Valid WithdrawRequest depositWithdraw) {
        walletService.withdraw(depositWithdraw);
        return Response.ok().build();
    }

    @POST
    @Path("/transfer")
    @Produces(APPLICATION_JSON)
    @Operation(summary = "Facilitate the transfer of money between user wallets")
    public Response transfer(@Valid TransferRequest transferWithdraw) {
        walletService.transfer(transferWithdraw);
        return Response.ok().build();
    }
}
