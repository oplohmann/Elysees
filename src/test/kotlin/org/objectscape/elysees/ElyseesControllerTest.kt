package org.objectscape.elysees

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.objectscape.elysees.configuration.ElyseesProperties
import org.objectscape.elysees.leases.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit


@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ExtendWith(SpringExtension::class)
@TestPropertySource(locations = ["classpath:application-test.properties"])
class ElyseesControllerTest : ServerSideTest() {

    companion object {
        val MAX_PROCESSING_TIME = TimeUnit.SECONDS.toMillis(5)
        const val LEASE_NAME = "someLeaseName"
        const val NEVER_USED_LEASE_NAME = "someOtherLeaseName"
    }

    @Autowired
    private lateinit var leaseStore : LeaseStore

    @Autowired
    private lateinit var elyseesProperties: ElyseesProperties

    private var restTemplate: RestTemplate = RestTemplate()

    @BeforeEach
    fun setUp() {
        leaseStore.clear()
    }

    @Test
    fun isalive() {
        val url = appendToUrl("isalive") // http://localhost:8080/elysees-api/v1/isalive
        val responseEntity = restTemplate.getForEntity(url, String::class.java)
        assertEquals(HttpStatus.OK, responseEntity.statusCode)
        val body = responseEntity.body
        assertNotNull(body)
        assertDoesNotThrow { LocalDateTime.parse(body) } // verify that we received a valid date string
    }

    @Test
    fun badRequest() {
        val url = appendToUrl("requestLease")
        val leaseCreationData = LeaseCreationRequest("", 0)
        var badRequestExceptionOccurred = false

        try {
            restTemplate.postForEntity(url, leaseCreationData, LeaseCreationResponse::class.java)
        } catch (e: HttpClientErrorException) {
            badRequestExceptionOccurred = true
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
            assertTrue(e.message!!.contains("leaseName must nor be null or empty!"))
            assertTrue(e.message!!.contains("durationInMS must not be negative or 0!"))
        }

        assertTrue(badRequestExceptionOccurred)
    }

    @Test
    fun requestLease() {
        val requestedLeaseDuration = TimeUnit.SECONDS.toMillis(60)
        val response = submitRequestLeaseCall(requestedLeaseDuration)

        assertNotNull(response)
        if (response != null) {
            assertEquals("", response.badRequest)
            assertTrue(response.granted)
            assertTrue(response.token.length == 36) // length of a UUID
            assertEquals(requestedLeaseDuration, response.grantedDurationInMillis)
            assertEquals(requestedLeaseDuration, response.durationTillExpiry)
        }
    }

    @Test
    fun requestNewLeaseWithinLeaseDuration() {
        requestLease()
        val requestedLeaseDuration = TimeUnit.SECONDS.toMillis(60)
        val response = submitRequestLeaseCall(requestedLeaseDuration)
        assertNotNull(response)
        if (response != null) {
            assertEquals(LEASE_NAME, response.leaseName)
            assertEquals("", response.badRequest)
            assertFalse(response.granted) // already granted lease not yet expired
            assertEquals("", response.token)
            assertEquals(0, response.grantedDurationInMillis)
            assertTrue(response.durationTillExpiry > 0)
            assertTrue(response.durationTillExpiry > requestedLeaseDuration - MAX_PROCESSING_TIME)
        }
    }

    @Test
    fun requestNewLeaseAfterLeaseDuration() {
        // Request a lease that was already requested. but of which the lease duration has already expired.
        val veryShortLeaseDuration = TimeUnit.MILLISECONDS.toMillis(1)
        val response = submitRequestLeaseCall(veryShortLeaseDuration)
        assertNotNull(response)

        // Submit a request for a lease that meanwhile has expired
        val requestedLeaseDuration = TimeUnit.SECONDS.toMillis(60)
        val renewedLeaseResponse = submitRequestLeaseCall(requestedLeaseDuration)
        assertNotNull(renewedLeaseResponse)

        if (renewedLeaseResponse != null) {
            assertEquals(LEASE_NAME, renewedLeaseResponse.leaseName)
            assertEquals("", renewedLeaseResponse.badRequest)
            assertTrue(renewedLeaseResponse.granted) // already granted lease not yet expired
            assertTrue(renewedLeaseResponse.token.length == 36) // length of a UUID
            assertEquals(requestedLeaseDuration, renewedLeaseResponse.grantedDurationInMillis)
            assertEquals(requestedLeaseDuration, renewedLeaseResponse.durationTillExpiry)
        }
    }

    @Test
    fun declineLease() {
        val veryLongLeaseDuration = TimeUnit.SECONDS.toMillis(6000)
        val response = submitRequestLeaseCall(veryLongLeaseDuration)
        assertNotNull(response)

        val renewLeaseResponse = submitRequestLeaseCall(veryLongLeaseDuration)
        assertNotNull(renewLeaseResponse)

        if (renewLeaseResponse != null) {
            assertEquals("", renewLeaseResponse.badRequest)
            assertEquals("", renewLeaseResponse.token)
            assertFalse(renewLeaseResponse.granted)
            assertEquals(0, renewLeaseResponse.grantedDurationInMillis)
            assertTrue(renewLeaseResponse.durationTillExpiry > 0)
            assertTrue(renewLeaseResponse.durationTillExpiry > veryLongLeaseDuration - MAX_PROCESSING_TIME)
        }
    }

    @Test
    fun queryDurationTillExpiry() {
        val requestedLeaseDuration = TimeUnit.SECONDS.toMillis(60)
        val response = submitRequestLeaseCall(requestedLeaseDuration)
        assertNotNull(response)

        val url = appendToUrl("durationTillExpiry?leaseName=$LEASE_NAME")
        val responseEntity = restTemplate.getForEntity(url, LeaseExpiryResponse::class.java)
        assertEquals(HttpStatus.OK, responseEntity.statusCode)
        val body = responseEntity.body
        assertNotNull(body)

        if (body != null) {
            assertEquals("", body.badRequest)
            assertEquals(LEASE_NAME, body.leaseName)
            assertTrue(body.leaseFound)
            assertTrue(body.durationTillExpiry < requestedLeaseDuration)
            assertTrue(body.durationTillExpiry > requestedLeaseDuration - MAX_PROCESSING_TIME)
        }
    }

    @Test
    fun queryDurationTillExpiryMustBeZero() {
        val requestedLeaseDuration = 1L
        val response = submitRequestLeaseCall(requestedLeaseDuration)
        assertNotNull(response)

        Thread.sleep(1) // make sure lease is for expired when durationTillExpiry is called

        val url = appendToUrl("durationTillExpiry?leaseName=$LEASE_NAME")
        val responseEntity = restTemplate.getForEntity(url, LeaseExpiryResponse::class.java)
        assertEquals(HttpStatus.OK, responseEntity.statusCode)
        val body = responseEntity.body
        assertNotNull(body)

        if (body != null) {
            assertEquals("", body.badRequest)
            assertEquals(LEASE_NAME, body.leaseName)
            assertEquals(0, body.durationTillExpiry)
        }
    }

    @Test
    fun queryDurationTillExpiryInexistentLeaseName() {
        val inexistentLeaseName = "foobar"
        val url = appendToUrl("durationTillExpiry?leaseName=$inexistentLeaseName")
        val responseEntity = restTemplate.getForEntity(url, LeaseExpiryResponse::class.java)
        assertEquals(HttpStatus.OK, responseEntity.statusCode)
        val body = responseEntity.body
        assertNotNull(body)

        if (body != null) {
            assertEquals("", body.badRequest)
            assertEquals(inexistentLeaseName, body.leaseName)
            assertFalse(body.leaseFound)
            assertEquals(0, body.durationTillExpiry)
        }
    }

    @Test
    fun queryDurationTillExpiryInvalidCall() {
        val missingLeaseName = ""
        val url = appendToUrl("durationTillExpiry?leaseName=$missingLeaseName")

        var badRequestExceptionOccurred = false
        try {
            restTemplate.getForEntity(url, LeaseExpiryResponse::class.java)
        } catch (e: HttpClientErrorException) {
            badRequestExceptionOccurred = true
            assertEquals(HttpStatus.BAD_REQUEST, e.statusCode)
            assertTrue(e.message!!.contains("lease name must not be empty"))
        }

        assertTrue(badRequestExceptionOccurred)
    }

    @Test
    fun renewExistingNotExpiredLease() {
        val requestedLeaseDuration = TimeUnit.SECONDS.toMillis(60)
        val response = submitRequestLeaseCall(requestedLeaseDuration)
        if (response != null) {
            assertTrue(response.granted)
        }

        val url = appendToUrl("renewLease")
        val requestedLeaseExtensionInMillis = TimeUnit.SECONDS.toMillis(10)
        val leaseCreationData = LeaseRenewRequest(LEASE_NAME, requestedLeaseExtensionInMillis, response!!.token)
        val responseEntity = restTemplate.postForEntity(url, leaseCreationData, LeaseRenewResponse::class.java)
        val body = assertResponseEntity(responseEntity)

        if (body != null) {
            assertEquals("", body.badRequest)
            assertEquals(LEASE_NAME, body.leaseName)
            assertTrue(body.granted)
            assertFalse(body.heldByOtherLease)
            assertTrue(body.leaseFound)
            assertTrue(body.leaseFound)
            assertTrue(body.grantedDurationInMillis > requestedLeaseExtensionInMillis)
            assertTrue(body.grantedDurationInMillis < requestedLeaseDuration)
        }
    }

    @Test
    fun renewExistingExpiredLease() {
        val veryShortLeaseDuration = 1L
        val response = submitRequestLeaseCall(veryShortLeaseDuration)

        assertNotNull(response)
        if (response != null) {
            assertEquals("", response.badRequest)
            assertTrue(response.granted)
            assertTrue(response.token.length == 36) // length of a UUID
            assertEquals(veryShortLeaseDuration, response.grantedDurationInMillis)
        }

        val url = appendToUrl("renewLease")
        val leaseExtensionDuration = TimeUnit.SECONDS.toMillis(10)
        val leaseCreationData = LeaseRenewRequest(LEASE_NAME, leaseExtensionDuration, response!!.token)
        val responseEntity = restTemplate.postForEntity(url, leaseCreationData, LeaseRenewResponse::class.java)
        assertNotNull(responseEntity)
        assertEquals(HttpStatus.OK, responseEntity.statusCode)
        val body = responseEntity.body
        assertNotNull(body)

        if (body != null) {
            assertEquals("", body.badRequest)
            assertEquals(LEASE_NAME, body.leaseName)
            assertTrue(body.granted)
            assertTrue(body.leaseFound)
            assertFalse(body.heldByOtherLease)
            assertTrue(body.grantedDurationInMillis > 0)
            assertTrue(leaseExtensionDuration == body.grantedDurationInMillis)
        }
    }

    @Test
    fun renewExistingLeaseNotBeingOwner() {
        val veryShortLeaseDuration = 1L
        val response = submitRequestLeaseCall(veryShortLeaseDuration)

        assertNotNull(response)
        if (response != null) {
            assertEquals("", response.badRequest)
            assertTrue(response.granted)
            assertTrue(response.token.length == 36) // length of a UUID
            assertEquals(veryShortLeaseDuration, response.grantedDurationInMillis)
        }

        val url = appendToUrl("renewLease")
        val leaseExtensionDuration = TimeUnit.SECONDS.toMillis(10)
        val otherRequesterToken = UUID.randomUUID().toString()
        val leaseCreationData = LeaseRenewRequest(LEASE_NAME, leaseExtensionDuration, otherRequesterToken)
        val responseEntity = restTemplate.postForEntity(url, leaseCreationData, LeaseRenewResponse::class.java)

        val body = assertResponseEntity(responseEntity)
        if (body != null) {
            assertEquals("", body.badRequest)
            assertEquals(LEASE_NAME, body.leaseName)
            assertFalse(body.granted)
            assertTrue(body.heldByOtherLease)
            assertTrue(body.leaseFound)
            assertEquals(0, body.grantedDurationInMillis)
        }
    }

    @Test
    fun renewNonExistingLease() {
        val url = appendToUrl("renewLease")

        val anyRandomFakeToken = UUID.randomUUID().toString()
        val leaseCreationData = LeaseRenewRequest(NEVER_USED_LEASE_NAME, TimeUnit.SECONDS.toMillis(10), anyRandomFakeToken)
        val responseEntity = restTemplate.postForEntity(url, leaseCreationData, LeaseRenewResponse::class.java)
        val body = assertResponseEntity(responseEntity)

        if (body != null) {
            assertEquals("", body.badRequest)
            assertEquals(NEVER_USED_LEASE_NAME, body.leaseName)
            assertFalse(body.granted)
            assertFalse(body.heldByOtherLease)
            assertFalse(body.leaseFound)
            assertEquals(0, body.grantedDurationInMillis)
        }
    }

    @Test
    fun releaseExistingLease() {
        val requestedLeaseDuration = TimeUnit.SECONDS.toMillis(60)
        val response = submitRequestLeaseCall(requestedLeaseDuration) ?: return

        val url = appendToUrl("releaseLease")

        val leaseReleaseRequest = LeaseReleaseRequest(LEASE_NAME, response.token)
        val responseEntity = restTemplate.postForEntity(url, leaseReleaseRequest, LeaseReleaseResponse::class.java)
        val body = assertResponseEntity(responseEntity)

        if (body != null) {
            assertEquals("", body.badRequest)
            assertEquals(LEASE_NAME, body.leaseName)
            assertTrue(body.leaseFound)
            assertTrue(body.releaseSuccessful)
            assertTrue(body.leaseOwner)
        }
    }

    @Test
    fun releaseExistingLeaseNotBeingOwner() {
        val requestedLeaseDuration = TimeUnit.SECONDS.toMillis(60)
        submitRequestLeaseCall(requestedLeaseDuration) ?: return

        val url = appendToUrl("releaseLease")

        val anyRandomFakeToken = UUID.randomUUID().toString()
        val leaseReleaseRequest = LeaseReleaseRequest(LEASE_NAME, anyRandomFakeToken)
        val responseEntity = restTemplate.postForEntity(url, leaseReleaseRequest, LeaseReleaseResponse::class.java)
        val body = assertResponseEntity(responseEntity)

        if (body != null) {
            assertEquals("", body.badRequest)
            assertEquals(LEASE_NAME, body.leaseName)
            assertTrue(body.leaseFound)
            assertFalse(body.releaseSuccessful)
            assertFalse(body.leaseOwner)
        }
    }

    @Test
    fun releaseNotExistingLease() {
        val requestedLeaseDuration = TimeUnit.SECONDS.toMillis(60)
        val response = submitRequestLeaseCall(requestedLeaseDuration) ?: return

        val url = appendToUrl("releaseLease")

        val leaseReleaseRequest = LeaseReleaseRequest(NEVER_USED_LEASE_NAME, response.token)
        val responseEntity = restTemplate.postForEntity(url, leaseReleaseRequest, LeaseReleaseResponse::class.java)
        val body = assertResponseEntity(responseEntity)

        if (body != null) {
            assertEquals("", body.badRequest)
            assertEquals(NEVER_USED_LEASE_NAME, body.leaseName)
            assertFalse(body.leaseFound)
            assertFalse(body.releaseSuccessful)
            assertFalse(body.leaseOwner)
        }
    }

    private fun <T> assertResponseEntity(responseEntity: ResponseEntity<T>): T? {
        assertNotNull(responseEntity)
        assertEquals(HttpStatus.OK, responseEntity.statusCode)
        val body = responseEntity.body
        assertNotNull(body)
        return body
    }

    private fun submitRequestLeaseCall(leaseDurationInMillis: Long): LeaseCreationResponse? {
        val url = appendToUrl("requestLease")
        val leaseCreationData = LeaseCreationRequest(LEASE_NAME, leaseDurationInMillis)
        val responseEntity = restTemplate.postForEntity(url, leaseCreationData, LeaseCreationResponse::class.java)
        assertNotNull(responseEntity)
        assertEquals(HttpStatus.OK, responseEntity.statusCode)
        val body = responseEntity.body
        assertNotNull(body)
        return body
    }

    private fun appendToUrl(callLabel: String) = elyseesProperties.url + "/" + callLabel

}