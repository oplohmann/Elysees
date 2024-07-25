package org.objectscape.elysees

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.objectscape.elysees.local.Lease
import java.util.concurrent.TimeUnit

class LocalLeaseTest : ClientSideTest() {

    @Test
    fun requestRenewLease() {
        val lease: Lease = elysees.requestLease(SHARED_NETWORK_RESOURCE, TimeUnit.SECONDS.toMillis(4))
        lease.ifNotGranted {
            assertNotNull(
                null,
                "test case is based on the assumption of lease with that name being requested for the first time")
        }

        var visitCount = 0
        var visitMaxCount = 100
        val oneSecond = TimeUnit.SECONDS.toMillis(1)

        while(visitCount < visitMaxCount) {
            visitCount++
            if(lease.renew(oneSecond)) {
                sharedDummyNetworkResource.put("foo", getSomeNewValue())
                doSomeOtherWork()
            }
        }

        assertEquals(visitMaxCount, visitCount)
    }

}