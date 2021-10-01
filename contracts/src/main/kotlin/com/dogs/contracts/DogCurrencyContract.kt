package com.dogs.contracts

import com.dogs.states.DogCurrency
import com.dogs.states.DogState
import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import net.corda.core.contracts.Contract
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class DogCurrencyContract : EvolvableTokenContract(), Contract {

    companion object {
        const val CONTRACT_ID = "com.dogs.contracts.DogCurrencyContract"
    }

    /** For CorDapp developers to implement. */
    override fun additionalCreateChecks(tx: LedgerTransaction) {
        println("additional create checks")
        val newToken = tx.outputStates.single() as DogCurrency
        requireThat {
            "Token can be created by Regulator.".using("Regulator".equals(newToken.maintainer.name.organisation))
        }

    }

    /** For CorDapp developers to implement. */
    override fun additionalUpdateChecks(tx: LedgerTransaction) {
        println("additional update checks")
//        val oldToken = tx.inputStates.single() as DogCurrency
//        val newToken = tx.outputStates.single() as DogCurrency
//        requireThat {
//            "Token value should be greater than 0.".using(newToken.value >= 0)
//        }
    }

}