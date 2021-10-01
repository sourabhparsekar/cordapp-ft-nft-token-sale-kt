package com.dogs.states

import com.dogs.contracts.DogTokenContract
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

@BelongsToContract(DogTokenContract::class)
class DogState (
        val id: String,
        val assignedTo: Party,
        override val linearId: UniqueIdentifier,
        override val fractionDigits: Int,
        override val maintainers: List<Party> = listOf(assignedTo)
) : EvolvableTokenType()