package com.dogs.states

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import org.junit.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito

class DogCurrencyTest {

     @Test
     fun testEquals(){

         val party = Mockito.mock(Party::class.java)

         val dogCurrency1 = DogCurrency(UniqueIdentifier(), 0, party)
         val dogCurrency2 = DogCurrency(UniqueIdentifier(), 0, party)

         assertTrue(dogCurrency1 == dogCurrency2)

     }


 }
