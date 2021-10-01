package com.dogs.flows

import co.paralleluniverse.fibers.Suspendable
import com.dogs.states.DogCurrency
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.utilities.ProgressTracker
import java.util.concurrent.atomic.AtomicInteger

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class GetDogCurrencyBalance(
    private val maintainer: String
) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): String {

        //get states on ledger
        val stateAndRef = serviceHub.vaultService.queryBy(DogCurrency::class.java).states

        val totalTokenAvailable = AtomicInteger()

        stateAndRef.forEach {

            //get the Token State object
            val evolvableTokenType = it.state.data

            if (maintainer.equals(evolvableTokenType.maintainer.name.organisation)) {

                //get the pointer pointer to the currency
                val tokenPointer: TokenPointer<DogCurrency> = evolvableTokenType.toPointer(evolvableTokenType.javaClass)

                //retrieve amount
                val amount: Amount<TokenType> = serviceHub.vaultService.tokenBalance(tokenPointer)

                totalTokenAvailable.set(totalTokenAvailable.get() + amount.quantity.toInt())
            }
        }

        return "Balance is ${totalTokenAvailable}. Tokens issued by $maintainer";

    }
}