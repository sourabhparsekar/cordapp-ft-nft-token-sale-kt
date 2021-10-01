package com.dogs

import com.dogs.flows.*
import com.dogs.states.DogState
import com.r3.corda.lib.tokens.selection.InsufficientBalanceException
import net.corda.core.node.services.Vault.StateStatus
import net.corda.core.node.services.vault.QueryCriteria
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals


class FlowTests : AbstractFlowConfiguration() {

    @Test
    fun `create a new dog non fungible token`() {

        val addNewDogAssetFlow = CreateNewDogToken("AB137D")

        regulatorNode.startFlow(addNewDogAssetFlow).toCompletableFuture()

        mockNetwork.waitQuiescent()

        val queryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(StateStatus.UNCONSUMED)

        assertThrows<NoSuchElementException> {
            partyANode.services.vaultService.queryBy(DogState::class.java).states.first {
                it.state.data.id.equals("AB137D")
            }
        }

        assertThrows<NoSuchElementException> {
            partyBNode.services.vaultService.queryBy(DogState::class.java).states.first {
                it.state.data.id.equals("AB137D")
            }
        }

    }

    @Test
    fun `create a new dog token and assign owner`() {

        // create a new dog

        val dogTagId: String = "FC147D"

        val addNewDogAssetFlow = CreateNewDogToken(dogTagId)

        regulatorNode.startFlow(addNewDogAssetFlow).toCompletableFuture()

        mockNetwork.waitQuiescent()

        val queryCriteria = QueryCriteria.VaultQueryCriteria().withStatus(StateStatus.UNCONSUMED)

        assertThrows<NoSuchElementException> {
            partyANode.services.vaultService.queryBy(DogState::class.java).states.first {
                it.state.data.id.equals(dogTagId)
            }
        }

        assertThrows<NoSuchElementException> {
            partyBNode.services.vaultService.queryBy(DogState::class.java).states.first {
                it.state.data.id.equals(dogTagId)
            }
        }

        // assign the new dog to party a

        val assignOwner = AssignOwner(dogTagId, aParty.name.organisation)

        regulatorNode.startFlow(assignOwner).toCompletableFuture()

        mockNetwork.waitQuiescent()

        assertThrows<NoSuchElementException> {
            partyBNode.services.vaultService.queryBy(DogState::class.java).states.first {
                it.state.data.id.equals(dogTagId)
            }
        }
    }


    @Test
    fun `create a new dog currency fungible token`() {

        val createDogCurrency = IssueDogCurrencyFlow(
            100,
            aParty.name.organisation
        )

        val future = regulatorNode.startFlow(createDogCurrency).toCompletableFuture()

        mockNetwork.waitQuiescent()

        println(future.get())

        val currencyFuture = partyANode.startFlow(GetDogCurrencyBalance("Regulator")).toCompletableFuture()

        mockNetwork.waitQuiescent()

        val out = currencyFuture.get()

        println(out)

        assertEquals("Balance is 100. Tokens issued by Regulator", out)

    }


    @Test
    fun `sell a non fungible dog token to a party using fungible dog currency token`() {

        // create a new dog

        val dogTagId: String = "FC147D"

        val addNewDogAssetFlow = CreateNewDogToken(dogTagId)

        regulatorNode.startFlow(addNewDogAssetFlow).toCompletableFuture()

        mockNetwork.waitQuiescent()


        // create dog currency and allocate it to Party A
        val buyer = aParty.name.organisation

        val createDogCurrency = IssueDogCurrencyFlow(
            100,
            buyer
        )

        val future = regulatorNode.startFlow(createDogCurrency).toCompletableFuture()

        mockNetwork.waitQuiescent()

        println(future.get())

        var currencyFuture = partyANode.startFlow(GetDogCurrencyBalance("Regulator")).toCompletableFuture()

        mockNetwork.waitQuiescent()

        var output = currencyFuture.get()

        println(output)

        assertEquals("Balance is 100. Tokens issued by Regulator", output)

        // sell dog to party A in exchange of dog currency

        val sellDogWithDogCurrency = SellDogWithDogCurrency(
            dogTagId = dogTagId,
            buyer = buyer,
            price = 59
        )

        val saleFuture = regulatorNode.startFlow(sellDogWithDogCurrency).toCompletableFuture()

        mockNetwork.waitQuiescent()

        output = saleFuture.get()

        println(output)

        currencyFuture = partyANode.startFlow(GetDogCurrencyBalance("Regulator")).toCompletableFuture()

        mockNetwork.waitQuiescent()

        output = currencyFuture.get()

        println(output)

        assertEquals("Balance is 41. Tokens issued by Regulator", output)


        currencyFuture = regulatorNode.startFlow(GetDogCurrencyBalance("Regulator")).toCompletableFuture()

        mockNetwork.waitQuiescent()

        output = currencyFuture.get()

        println(output)

        assertEquals("Balance is 59. Tokens issued by Regulator", output)

    }


    @Test
    fun `sell a non fungible dog token to a party using 2 fungible dog currency tokens`() {

        // create a new dog

        val dogTagId: String = "FC148D"

        val addNewDogAssetFlow = CreateNewDogToken(dogTagId)

        regulatorNode.startFlow(addNewDogAssetFlow).toCompletableFuture()

        mockNetwork.waitQuiescent()


        // create dog currency and allocate it to Party A
        val buyer = aParty.name.organisation

//        val createDogCurrency =

        val future1 = regulatorNode.startFlow(IssueDogCurrencyFlow(
            10,
            buyer
        )).toCompletableFuture()

        mockNetwork.waitQuiescent()

        println(future1.get())

        val future2 = regulatorNode.startFlow(IssueDogCurrencyFlow(
            50,
            buyer
        )).toCompletableFuture()

        mockNetwork.waitQuiescent()

        println(future2.get())

        // sell dog to party A in exchange of dog currency

        val sellDogWithDogCurrency = SellDogWithDogCurrency(
            dogTagId = dogTagId,
            buyer = buyer,
            price = 59
        )

        val saleFuture = regulatorNode.startFlow(sellDogWithDogCurrency).toCompletableFuture()

        mockNetwork.waitQuiescent()

        var output = saleFuture.get()

        println(output)

        var currencyFuture = partyANode.startFlow(GetDogCurrencyBalance("Regulator")).toCompletableFuture()

        mockNetwork.waitQuiescent()

        output = currencyFuture.get()

        println(output)

        assertEquals("Balance is 1. Tokens issued by Regulator", output)


        currencyFuture = regulatorNode.startFlow(GetDogCurrencyBalance("Regulator")).toCompletableFuture()

        mockNetwork.waitQuiescent()

        output = currencyFuture.get()

        println(output)

        assertEquals("Balance is 59. Tokens issued by Regulator", output)

    }

    @Test
    fun `sell a non fungible dog token to a party using less fungible dog currency token`() {

        // create a new dog

        val dogTagId: String = "FC148D"

        val addNewDogAssetFlow = CreateNewDogToken(dogTagId)

        regulatorNode.startFlow(addNewDogAssetFlow).toCompletableFuture()

        mockNetwork.waitQuiescent()


        // create dog currency and allocate it to Party A
        val buyer = bParty.name.organisation

        val createDogCurrency = IssueDogCurrencyFlow(
            50,
            buyer
        )

        val future = regulatorNode.startFlow(createDogCurrency).toCompletableFuture()

        mockNetwork.waitQuiescent()

        println(future.get())

        var currencyFuture = partyBNode.startFlow(GetDogCurrencyBalance("Regulator")).toCompletableFuture()

        mockNetwork.waitQuiescent()

        var output = currencyFuture.get()

        println(output)

        assertEquals("Balance is 50. Tokens issued by Regulator", output)

        // sell dog to party A in exchange of dog currency

        val sellDogWithDogCurrency = SellDogWithDogCurrency(
            dogTagId = dogTagId,
            buyer = buyer,
            price = 59
        )

        assertThrows<InsufficientBalanceException> {
            regulatorNode.startFlow(sellDogWithDogCurrency).toCompletableFuture()
            mockNetwork.waitQuiescent()
        }
//        output = saleFuture.get()
//
//        println(output)
//
//        currencyFuture = partyBNode.startFlow(GetDogCurrencyBalance()).toCompletableFuture()
//
//        mockNetwork.waitQuiescent()
//
//        output = currencyFuture.get()
//
//        println(output)
//
//        assertEquals("Balance is 41. Tokens issued by Regulator", output)
//
//
//        currencyFuture = regulatorNode.startFlow(GetDogCurrencyBalance()).toCompletableFuture()
//
//        mockNetwork.waitQuiescent()
//
//        output = currencyFuture.get()
//
//        println(output)
//
//        assertEquals("Balance is 59. Tokens issued by Regulator", output)

    }

}