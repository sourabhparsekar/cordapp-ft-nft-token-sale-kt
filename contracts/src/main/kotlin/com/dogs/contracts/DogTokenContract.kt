package com.dogs.contracts

import com.dogs.states.DogState
import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction


class DogTokenContract : EvolvableTokenContract(), Contract {

    companion object {
        const val CONTRACT_ID = "com.dogs.contracts.DogTokenContract"
    }

    /** For CorDapp developers to implement. */
    override fun additionalCreateChecks(tx: LedgerTransaction) {
        println("additional create checks")
        val newToken = tx.outputStates.single() as DogState
        requireThat {
            "Dog Tag Identifier cannot be empty".using(newToken.id.isNotBlank())
            "New Token can be created by only Regulator.".using(newToken.assignedTo.name.organisation.equals("Regulator"))
        }
    }

    /** For CorDapp developers to implement. */
    override fun additionalUpdateChecks(tx: LedgerTransaction) {
        println("additional update checks")
        val oldToken = tx.inputStates.single() as DogState
        val newToken = tx.outputStates.single() as DogState
        requireThat {
            "Dog Tag Identifier cannot be empty.".using(newToken.id.isNotBlank())
        }
    }

}
