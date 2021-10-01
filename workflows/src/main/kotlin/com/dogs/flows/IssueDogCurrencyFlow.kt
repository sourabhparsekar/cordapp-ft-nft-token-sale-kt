package com.dogs.flows

import co.paralleluniverse.fibers.Suspendable
import com.dogs.states.DogCurrency
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.withNotary
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC

@StartableByRPC
class IssueDogCurrencyFlow(
    private val value: Int,
    private val owner: String
) : FlowLogic<String>() {
    /**
     * This is where you fill out your business logic.
     */
    @Suspendable
    override fun call(): String {

        val currency = serviceHub.vaultService.queryBy(DogCurrency::class.java).states

        var dogCurrency: DogCurrency

        if (currency.isEmpty()) {
            val notary = serviceHub.networkMapCache.notaryIdentities.single()

            //create token type
            dogCurrency = DogCurrency(
                UniqueIdentifier(),
                0,
                ourIdentity
            )

            //warp it with transaction state specifying the notary
            val transactionState = dogCurrency withNotary notary

            //call built in sub flow CreateEvolvableTokens. This can be called via rpc or in unit testing
            subFlow(CreateEvolvableTokens(transactionState))

            println("Fungible Dog Currency token type has been created with UUID : ${dogCurrency.linearId}")
        } else {
            dogCurrency = currency.first().state.data
        }

        /*
      * Create an instance of IssuedTokenType, it is used by our Fungible token which would be issued to the owner.
      * Note that the IssuedTokenType takes
      * a TokenPointer as an input, since EvolvableTokenType is not TokenType, but is a LinearState.
      * This is done to separate the state info from the token
      * so that the state can evolve independently.
      * IssuedTokenType is a wrapper around the TokenType and the issuer.
      * */

        // get the party
        val ownerParty = serviceHub.identityService.partiesFromName(owner, true).first()


        val issuedDogCurrencyToken: IssuedTokenType = dogCurrency.toPointer(dogCurrency.javaClass) issuedBy ourIdentity

        /* Create an instance of the fungible token with the owner as the token holder.*/
        val dogCurrencyToken = FungibleToken(
            Amount(
                value.toLong(),
                issuedDogCurrencyToken
            ),
            holder = ownerParty
        )

        /* Issue the currency token by calling the IssueTokens flow provided with the TokenSDK */
        val stx = subFlow(IssueTokens(listOf(dogCurrencyToken), listOf(ownerParty)))

        return ("The fungible dog currency token created with UUID: ${dogCurrency.linearId} & Valuation: $value is assigned to ${ownerParty.name.organisation}"
                + "\nTransaction ID: ${stx.id}")


    }
}