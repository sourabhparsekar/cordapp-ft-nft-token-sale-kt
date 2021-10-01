package com.dogs

import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.AfterClass
import org.junit.BeforeClass

abstract class AbstractFlowConfiguration {

    companion object {

        lateinit var mockNetwork: MockNetwork
        lateinit var regulatorNode: StartedMockNode
        lateinit var partyANode: StartedMockNode
        lateinit var partyBNode: StartedMockNode

        lateinit var regulatorParty: Party
        lateinit var aParty: Party
        lateinit var bParty: Party


        object RegulatorNodeInfo {
            const val organization = "Regulator"
            const val locality = "Goa"
            const val country = "IN"
        }

        object PartyANodeInfo {
            const val organization = "PartyA"
            const val locality = "Goa"
            const val country = "IN"
        }

        object PartyBNodeInfo {
            const val organization = "PartyB"
            const val locality = "Goa"
            const val country = "IN"
        }

        @JvmStatic
        @BeforeClass
        fun setup() {

            val networkParameters = testNetworkParameters(minimumPlatformVersion = 4)

            val contractsCordapp = "com.dogs.contracts"
            val flowsCordapp = "com.dogs.flows"
            val tokenContractsCordapp = "com.r3.corda.lib.tokens.contracts"
            val tokenFlowsCordapp = "com.r3.corda.lib.tokens.workflows"

            mockNetwork = MockNetwork(
                MockNetworkParameters(
                    cordappsForAllNodes = listOf(
                        TestCordapp.findCordapp(contractsCordapp),
                        TestCordapp.findCordapp(flowsCordapp),
                        TestCordapp.findCordapp(tokenContractsCordapp),
                        TestCordapp.findCordapp(tokenFlowsCordapp)
                    ),
                    threadPerNode = true,
                    networkParameters = networkParameters
                )
            )

            regulatorNode = mockNetwork.createPartyNode(
                (CordaX500Name(
                    RegulatorNodeInfo.organization,
                    RegulatorNodeInfo.locality,
                    RegulatorNodeInfo.country
                ))
            )

            partyANode = mockNetwork.createPartyNode(
                (CordaX500Name(
                    PartyANodeInfo.organization,
                    PartyANodeInfo.locality,
                    PartyANodeInfo.country
                ))
            )

            partyBNode = mockNetwork.createPartyNode(
                (CordaX500Name(
                    PartyBNodeInfo.organization,
                    PartyBNodeInfo.locality,
                    PartyBNodeInfo.country
                ))
            )

            aParty = partyANode.info.singleIdentity()
            bParty = partyBNode.info.singleIdentity()
            regulatorParty = regulatorNode.info.singleIdentity()

            mockNetwork.startNodes()
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            mockNetwork.stopNodes()
        }
    }

}