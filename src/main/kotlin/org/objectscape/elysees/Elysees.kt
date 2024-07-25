package org.objectscape.elysees

import org.objectscape.elysees.leases.LeaseCreationRequest
import org.objectscape.elysees.leases.LeaseCreationResponse
import org.objectscape.elysees.leases.LeaseRenewRequest
import org.objectscape.elysees.leases.LeaseRenewResponse
import org.objectscape.elysees.local.Lease
import org.objectscape.elysees.local.LocalLease
import org.objectscape.elysees.local.NullLease
import org.objectscape.elysees.utils.ServerCommunicationException
import java.util.*

class Elysees : ElyseesUtility {

    companion object {
        fun forLeaseServer(urlPrefix: String): Elysees {
            return Elysees(urlPrefix)
        }
    }

    constructor(urlPrefix: String) : super(urlPrefix)

    @Throws(ServerCommunicationException::class)
    fun requestLease(leaseName: String, leaseDurationInMillis: Long) : Lease {
        val url = appendToUrl("requestLease")
        val leaseCreationData = LeaseCreationRequest(leaseName, leaseDurationInMillis)

        try {
            val now = System.currentTimeMillis()
            val responseEntity = submitForEntity(url, leaseCreationData, LeaseCreationResponse::class.java)
            val response = responseEntity.body

            if(response != null) {
                return LocalLease(
                    this,
                    response.leaseName,
                    response.token,
                    response.granted,
                    now,
                    response.grantedDurationInMillis,
                    response.durationTillExpiry)
            }
            return NullLease()
        } catch (e: Exception) {
            throw ServerCommunicationException(e.message, e)
        }
    }

    fun releaseLease(leaseName: String, token: String) {

    }

    @Throws(ServerCommunicationException::class)
    fun renewLease(leaseName: String, token: String, durationInMillis: Long): Long? {
        val url = appendToUrl("renewLease")

        val leaseCreationData = LeaseRenewRequest(leaseName, durationInMillis, token)
        val responseEntity = restTemplate.postForEntity(url, leaseCreationData, LeaseRenewResponse::class.java)
        val renewResponse = responseEntity.body

        if (renewResponse != null && renewResponse.granted) {
            return renewResponse.grantedDurationInMillis
        }

        return null
    }

}