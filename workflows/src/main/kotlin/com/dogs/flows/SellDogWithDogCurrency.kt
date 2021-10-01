package com.dogs.flows

import co.paralleluniverse.fibers.Suspendable
import com.dogs.states.DogCurrency
import com.dogs.states.DogState
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.r3.corda.lib.tokens.workflows.utilities.tokenBalance
import net.corda.core.contracts.Amount
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.internal.isUploaderTrusted
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
import kotlin.collections.ArrayList


// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class SellDogWithDogCurrency(
    private val dogTagId: String,
    private val buyer: String,
    private val price: Int //move this price to dog state itself 
) : FlowLogic<String>() {


    @Suspendable
    override fun call(): String {

        val notary = serviceHub.networkMapCache.notaryIdentities.single()

        // get reference to the NFT
        val dogStateAndRef =
            serviceHub.vaultService.queryBy<DogState>().states.first { it.state.data.id.equals(dogTagId) }

        // get token object
        val dogState = dogStateAndRef.state.data

        /* Build the transaction builder */
        val txBuilder = TransactionBuilder(notary)

        /**
         * Create a move token proposal for the dog token using the helper function provided by Token SDK.
         * This would create the movement proposal and would
         * be committed in the ledgers of parties once the transaction in finalized.
         **/
        // get pointer to the state
        val dogStatePointer: TokenPointer<*> = dogState.toPointer(dogState.javaClass)

        // get the party
        val buyerParty = serviceHub.identityService.partiesFromName(buyer, true).first()

        addMoveNonFungibleTokens(txBuilder, serviceHub, dogStatePointer, buyerParty)

        /* Initiate a flow session with the buyer to send the house valuation and transfer of the fiat currency */
        val buyerSession = initiateFlow(buyerParty)

        // Send the dog price to the buyer.
        buyerSession.send(price)

        // Receive inputStatesAndRef for the currency exchange from the buyer, these would be inputs to the fiat currency exchange transaction.
        val inputs = subFlow(ReceiveStateAndRefFlow<FungibleToken>(buyerSession))

        // Receive output for the fiat currency from the buyer, this would contain the transfered amount from buyer to yourself
        val moneyReceived: List<FungibleToken> = buyerSession.receive<List<FungibleToken>>().unwrap { it -> it }

        /* Create a fiat currency proposal for the dog token using the helper function provided by Token SDK. */
        addMoveTokens(txBuilder, inputs, moneyReceived)

        /* Sign the transaction with your private */
        val initialSignedTrnx = serviceHub.signInitialTransaction(txBuilder)

        /* Call the CollectSignaturesFlow to receive signature of the buyer */
        val ftx = subFlow(CollectSignaturesFlow(initialSignedTrnx, listOf(buyerSession)))

        /* Call finality flow to notarise the transaction */
        val stx = subFlow(FinalityFlow(ftx, listOf(buyerSession)))

        /* Distribution list is a list of identities that should receive updates. For this mechanism to behave correctly we call the UpdateDistributionListFlow flow */
        subFlow(UpdateDistributionListFlow(stx))

        return ("The dog is sold to " + buyerParty.name.organisation + "\nTransaction ID: "
                + stx.id)
    }
}

@InitiatedBy(SellDogWithDogCurrency::class)
class SellDogWithDogCurrencyResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {

//        //get states on ledger
//        val stateAndRef = serviceHub.vaultService.queryBy(DogCurrency::class.java).states.first()
//
//        //get the Token State object
//        val evolvableTokenType = stateAndRef.state.data
//
//        //get the pointer pointer to the currency
//        val tokenPointer: TokenPointer<DogCurrency> = evolvableTokenType.toPointer(evolvableTokenType.javaClass)
//
//        //retrieve amount
//        val amount: Amount<TokenType> = serviceHub.vaultService.tokenBalance(tokenPointer)

        //get states on ledger
        val stateAndRefList = serviceHub.vaultService.queryBy(DogCurrency::class.java).states
        //We use VaultService to fetch all TokenState.
        // This would by default return all UNCONSUMED tokens.

        // we just want tokens that are issued by a particular party i.e. the issuer
        // But, we can’t just put all of the tokens issued by our issuer as the input,
        // we just need the number of tokens requested to be transferred.
        // Also, we need to validate that there is enough token available to spend.
        // We can’t transfer the required amount of token’s if it isn’t available. So let’s do that:

        val totalTokenAvailable = AtomicInteger()

        /* Receive the valuation of the dog */
        var price = counterpartySession.receive<Int>().unwrap { it }

        val tokenStateAndRefs: List<StateAndRef<DogCurrency>> = stateAndRefList.stream()
            .filter {
                if (it.state.data.maintainer.name.organisation.equals("Regulator")) {

                    //Filter inputStates for spending
                    if (totalTokenAvailable.get() > price)
                        return@filter false

                    //get the Token State object
                    val evolvableTokenType = it.state.data

                    //get the pointer pointer to the currency
                    val tokenPointer: TokenPointer<DogCurrency> =
                        evolvableTokenType.toPointer(evolvableTokenType.javaClass)

                    //retrieve amount
                    val amount: Amount<TokenType> = serviceHub.vaultService.tokenBalance(tokenPointer)

                    //Calculate total tokens available
                    totalTokenAvailable.set(
                        (totalTokenAvailable.get() + amount.quantity).toInt()
                    )
                    return@filter true
                }
                false
            }.collect(Collectors.toList())

        // Validate if there is sufficient tokens to spend
        if (totalTokenAvailable.get() < price) {
            throw FlowException("Insufficient balance")
        }


        val partyAndAmountList: MutableList<Pair<Party, Amount<TokenType>>> = mutableListOf()

        tokenStateAndRefs.forEach {

            //get the Token State object
            val evolvableTokenType = it.state.data

            //get the pointer pointer to the currency
            val tokenPointer: TokenPointer<DogCurrency> = evolvableTokenType.toPointer(evolvableTokenType.javaClass)

            //retrieve amount
            val amount: Amount<TokenType> = serviceHub.vaultService.tokenBalance(tokenPointer)

            /* Create instance of the fiat currency token amount */
            /*
            price 59
            token 100

            if token >= price --> price
            if token < price --> token, price - token
             */

            val tokenQuantity = if( amount.quantity >= price ) price.toLong() else amount.quantity

            price -= amount.quantity.toInt()

            val priceToken = Amount(tokenQuantity, amount.token)

            println("Token Qty : $tokenQuantity")
            println("Amount Qty : ${amount.quantity}")
            println("Price Qty : ${priceToken.quantity}")

            /*
            *  Generate the move proposal, it returns the input-output pair for the currency transfer, which we need to send to the Initiator.
            * */
//            val partyAndAmount = PartyAndAmount(counterpartySession.counterparty, amount)

            partyAndAmountList.add(
                Pair(
                    counterpartySession.counterparty,
                    priceToken
                )
            )

        }


        val inputsAndOutputs: Pair<List<StateAndRef<FungibleToken>>, List<FungibleToken>> =
            DatabaseTokenSelection(serviceHub)
                .generateMove(
//                    listOf(
//                        Pair(
//                            counterpartySession.counterparty,
//                            amount
//                        )
//                    ),
                    partyAndAmountList,
                    ourIdentity
                )

        /* Call SendStateAndRefFlow to send the inputs to the Initiator*/
        subFlow(SendStateAndRefFlow(counterpartySession, inputsAndOutputs.first))

        /* Send the output generated from the fiat currency move proposal to the initiator */
        counterpartySession.send(inputsAndOutputs.second)

        //signing
        subFlow(object : SignTransactionFlow(counterpartySession) {
            @Throws(FlowException::class)
            override fun checkTransaction(stx: SignedTransaction) { // Custom Logic to validate transaction.
            }
        })

        return subFlow(ReceiveFinalityFlow(counterpartySession))
    }
}
