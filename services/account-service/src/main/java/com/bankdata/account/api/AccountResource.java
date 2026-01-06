package com.bankdata.account.api;

import com.bankdata.account.api.dto.*;
import com.bankdata.account.application.AccountService;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Accounts", description = "Account operations: create, deposit, transfer and balance.")
public class AccountResource {

    private final AccountService service;

    public AccountResource(AccountService service) {
        this.service = service;
    }

    @POST
    @Path("/accounts")
    @Operation(
            summary = "Create account",
            description = "Creates a new account with an initial deposit (defaults to 0.00 if omitted)."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "201",
                    description = "Account created",
                    content = @Content(schema = @Schema(implementation = CreateAccountResponse.class))
            ),
            @APIResponse(responseCode = "400", description = "Invalid request payload"),
            @APIResponse(responseCode = "409", description = "Account number collision (rare, retried internally)"),
            @APIResponse(responseCode = "500", description = "Unexpected error")
    })
    public Response create(@Valid CreateAccountRequest req) {
        CreateAccountResponse created = service.create(req);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @POST
    @Path("/accounts/{accountNumber}/deposit")
    @Operation(
            summary = "Deposit money",
            description = "Deposits money to the account. Amount must be non-negative and have max 2 decimal places."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Deposit applied",
                    content = @Content(schema = @Schema(implementation = BalanceResponse.class))
            ),
            @APIResponse(responseCode = "400", description = "Invalid amount"),
            @APIResponse(responseCode = "404", description = "Account not found"),
            @APIResponse(responseCode = "500", description = "Unexpected error")
    })
    public BalanceResponse deposit(@PathParam("accountNumber") String accountNumber, @Valid DepositRequest req) {
        return service.deposit(accountNumber, req);
    }

    @POST
    @Path("/accounts/transfer")
    @Operation(
            summary = "Transfer money",
            description = "Transfers money between two accounts. fromAccountNumber and toAccountNumber must be different."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Transfer applied",
                    content = @Content(schema = @Schema(implementation = TransferResponse.class))
            ),
            @APIResponse(responseCode = "400", description = "Invalid request (e.g. same account, invalid amount)"),
            @APIResponse(responseCode = "404", description = "One or both accounts not found"),
            @APIResponse(responseCode = "409", description = "Concurrency conflict / lock timeout (if mapped)"),
            @APIResponse(responseCode = "500", description = "Unexpected error")
    })
    public TransferResponse transfer(@Valid TransferRequest req) {
        return service.transfer(req);
    }

    @GET
    @Path("/accounts/{accountNumber}/balance")
    @Operation(
            summary = "Get balance",
            description = "Returns current balance for the specified account."
    )
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Balance returned",
                    content = @Content(schema = @Schema(implementation = BalanceResponse.class))
            ),
            @APIResponse(responseCode = "404", description = "Account not found"),
            @APIResponse(responseCode = "500", description = "Unexpected error")
    })
    public BalanceResponse balance(@PathParam("accountNumber") String accountNumber) {
        return service.balance(accountNumber);
    }
}