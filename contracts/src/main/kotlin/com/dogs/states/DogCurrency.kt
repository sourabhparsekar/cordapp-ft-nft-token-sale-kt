package com.dogs.states

import com.dogs.contracts.DogCurrencyContract
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(DogCurrencyContract::class)
class DogCurrency (
    override val linearId: UniqueIdentifier,
    override val fractionDigits: Int,
    val maintainer: Party,
    override val maintainers: List<Party> = listOf(maintainer)
) : EvolvableTokenType()
