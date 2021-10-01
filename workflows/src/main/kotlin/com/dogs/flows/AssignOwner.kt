package com.dogs.flows

import co.paralleluniverse.fibers.Suspendable
import com.dogs.states.DogState
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlowHandler
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import net.corda.core.flows.*
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
class AssignOwner(
    val dogTagId: String,
    val owner: String
) : FlowLogic<String>() {
    /**
     * This is where you fill out your business logic.
     */
    @Suspendable
    override fun call(): String {

        // get reference to the NFT
        val dogStateAndRef =
            serviceHub.vaultService.queryBy<DogState>().states.first { it.state.data.id.equals(dogTagId) }

        // get token object
        val dogState = dogStateAndRef.state.data

        // get pointer to the state
        val dogStatePointer: TokenPointer<*> = dogState.toPointer(dogState.javaClass)

        // get the party
        val ownerParty = serviceHub.identityService.partiesFromName(owner, true).first()

        //send tokens
        val session = initiateFlow(ownerParty)

        val txBuilder = TransactionBuilder(getPreferredNotary(serviceHub))

        addMoveNonFungibleTokens(txBuilder, serviceHub, dogStatePointer, ownerParty)

        val ptx = serviceHub.signInitialTransaction(txBuilder)

        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(session)))

        val ftx = subFlow(ObserverAwareFinalityFlow(stx, listOf(session)))

        /* Distribution list is a list of identities that should receive updates. For this mechanism to behave correctly we call the UpdateDistributionListFlow flow */

        subFlow(UpdateDistributionListFlow(ftx))

        return ("\nTransfer ownership of a Dog (Tag Id#: $dogTagId) to Owner (${ownerParty.name.organisation}) \n Transaction IDs: ${ftx.id}")

    }
}

@InitiatedBy(AssignOwner::class)
class AssignOwnerResponder(val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.
        subFlow(ObserverAwareFinalityFlowHandler(counterpartySession));
    }
}