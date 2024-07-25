package org.objectscape.elysees

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.objectscape.elysees.configuration.ElyseesProperties
import org.objectscape.elysees.utils.max
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import kotlin.random.Random

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ExtendWith(SpringExtension::class)
@TestPropertySource(locations = ["classpath:application-test.properties"])
abstract class ClientSideTest {

    companion object {
        const val SHARED_NETWORK_RESOURCE = "sharedNetworkResourceLease"
        val RANDOMIZER = Random(System.currentTimeMillis())
    }

    @Autowired
    protected lateinit var elyseesProperties: ElyseesProperties
    protected lateinit var elysees: Elysees

    protected var sharedDummyNetworkResource = HashMap<String, Int>()

    protected fun getSomeNewValue(): Int = RANDOMIZER.nextInt(1_000)

    protected fun doSomeOtherWork() {
        Thread.sleep(RANDOMIZER.nextLong(200).max(10))
    }

    @BeforeEach
    fun setUp() {
        elysees = Elysees.forLeaseServer(elyseesProperties.url)
    }

}