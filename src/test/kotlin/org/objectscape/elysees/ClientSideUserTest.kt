package org.objectscape.elysees

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.objectscape.elysees.local.Lease
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ExtendWith(SpringExtension::class)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class ClientSideUserTest : ClientSideTest() {

    @Test
    fun requestLease() {
        val tenSecondsLeaseDuration = TimeUnit.SECONDS.toMillis(10)
        val lease: Lease = elysees.requestLease(SHARED_NETWORK_RESOURCE, tenSecondsLeaseDuration)
        lease.nullIfGranted()?.let {
            // This is a demonstration test case where we know that the lease was obtained as there is no other
            // requester for that lease in this test case. Just to show how to make use of method nullIfGranted.
            return
        }

        // No problem with context switches at this line or further lines below as we are not going with locks,
        // but have obtained a lease that is reserved for 10s (tenSecondsLeaseDuration) which is way more enough
        // to carry out the following operations.

        val someSharedValue = RANDOMIZER.nextInt(1_000)
        lease.ifGranted {
            sharedDummyNetworkResource.put("foo", someSharedValue)
        }

        assertEquals(someSharedValue, sharedDummyNetworkResource.get("foo"))

        // You are not required to release a lease after access to some shared resource has finished,
        // but it improves throughput as some other thread trying to obtain that lease will be granted
        // access to the shared resource as early as possible.
        lease.release()
    }

    @Test
    fun renewLease() {
        val tenSeconds = TimeUnit.SECONDS.toMillis(10)
        val lease: Lease = elysees.requestLease(SHARED_NETWORK_RESOURCE, tenSeconds)
        lease.nullIfGranted()?.let {
            // This is a demonstration test case where we know that the lease was obtained as there is no other
            // requester for that lease in this test case. Just to show how to make use of method nullIfGranted.
            return
        }

        assertTrue(lease.isGranted())

        lease.ifGranted {
            sharedDummyNetworkResource.put("foo", RANDOMIZER.nextInt(1_000))
        }

        var visitCount = 0
        var visitMaxCount = 100
        val oneSecond = TimeUnit.SECONDS.toMillis(1)

        while(visitCount < visitMaxCount) {
            visitCount++
            if(lease.renew(oneSecond)) {
                sharedDummyNetworkResource.put("foo", getSomeNewValue())
                doSomeOtherWork()
            } else {
                // Renewing the lease was not possible as some other requester obtained it in the meanwhile.
                // Therefore, the lease has to be requested again from the beginning. The lease is requested
                // for <tenSeconds> for a retry period of <tenSeconds * 2>
                if(!lease.request(tenSeconds, tenSeconds * 2)) {
                    break // for this simple demonstration test case we just give up and exit
                }
            }
        }

        assertEquals(visitMaxCount, visitCount)
    }

    @Test
    fun leaseNotObtained() {
        val twoSeconds = TimeUnit.SECONDS.toMillis(2)
        val otherLease: Lease = elysees.requestLease(SHARED_NETWORK_RESOURCE, twoSeconds)
        assertTrue(otherLease.isGranted())

        var leaseWasFinallyObtained = false

        val lease: Lease = elysees.requestLease(SHARED_NETWORK_RESOURCE, twoSeconds)
        lease.ifNotGranted {
            // Lease <lease> not obtained as <otherLease> obtained it first and we have to
            // wait till its lease duration has expired or was returned by its own before
            // the end of the lease time. In this simple demonstration test case we just wait
            // for <lease.durationTillExpiry()> ms.
            doSomeOtherWorkForDuration(lease.durationTillExpiry())
            if(lease.request(twoSeconds, twoSeconds * 2)) {
                leaseWasFinallyObtained = true
            }
        }

        assertTrue(leaseWasFinallyObtained)
    }

    private fun doSomeOtherWorkForDuration(duration: Long) {
        Thread.sleep(duration)
    }

}