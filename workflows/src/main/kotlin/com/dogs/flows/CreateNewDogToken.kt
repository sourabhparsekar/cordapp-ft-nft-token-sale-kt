package com.dogs.flows

import co.paralleluniverse.fibers.Suspendable
import com.dogs.states.DogState
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.withNotary
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC

@StartableByRPC
class CreateNewDogToken(
    private val dogTagId: String
) : FlowLogic<String>() {
    /**
     * This is where you fill out your business logic.
     */
    @Suspendable
    override fun call(): String {

        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        val item = DogState(
            dogTagId,
            ourIdentity,
            UniqueIdentifier(),
            0
        )
        println("Non Fungible Dog Token Created.")

        //warp it with transaction state specifying the notary
        val transactionState = item withNotary notary

        subFlow(CreateEvolvableTokens(transactionState))

        println("Created Evolvable Dog Token. (Serial # $dogTagId).")


        /*
        * Create an instance of IssuedTokenType, it is used by our Non-Fungible token which would be issued to the owner.
        * Note that the IssuedTokenType takes
        * a TokenPointer as an input, since EvolvableTokenType is not TokenType, but is a LinearState.
        * This is done to separate the state info from the token
        * so that the state can evolve independently.
        * IssuedTokenType is a wrapper around the TokenType and the issuer.
        * */

        val issuedDogToken: IssuedTokenType = item.toPointer(item.javaClass) issuedBy ourIdentity

        /* Create an instance of the non-fungible token with the owner as the token holder.
         The last paramter is a hash of the jar containing the TokenType, use the helper function to fetch it. */
        val dogToken = NonFungibleToken(issuedDogToken, ourIdentity, UniqueIdentifier())

        /* Issue the house token by calling the IssueTokens flow provided with the TokenSDK */
        val stx = subFlow(IssueTokens(listOf(dogToken)))

        println("The non-fungible dog token (Dog Tag Id: ${item.id}) is created with UUID: ${item.linearId}"
        + "\nTransaction ID: ${stx.id}")

        return ("\nThe non-fungible dog token (Dog Tag Id: ${item.id}) is created with UUID: ${item.linearId}"
                + "\nTransaction ID: " + stx.id)

    }
}