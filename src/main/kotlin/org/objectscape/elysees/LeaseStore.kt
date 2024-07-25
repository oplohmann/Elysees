package org.objectscape.elysees

import org.objectscape.elysees.configuration.ElyseesProperties
import org.objectscape.elysees.leases.*
import org.objectscape.elysees.utils.LoggerFactory
import org.objectscape.elysees.utils.max
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

class LeaseStore {

    private val locks = ConcurrentHashMap<String, ReentrantLock>()
    private val leases = ConcurrentHashMap<String, LeaseData>()
    private var elyseesProperties: ElyseesProperties

    constructor(elyseesProperties: ElyseesProperties) {
        this.elyseesProperties = elyseesProperties
    }

    fun requestLease(leaseRequest: LeaseCreationRequest): LeaseCreationResponse {
        val now = System.currentTimeMillis()
        val lock = locks.computeIfAbsent(leaseRequest.leaseName) { ReentrantLock() }

        try {
            lock.lock()
            val leaseData = leases[leaseRequest.leaseName]
            if (leaseData == null) {
                val (newLeaseData, leaseResponse) = grantNewLease(now, leaseRequest)
                leases[leaseRequest.leaseName] = newLeaseData
                return leaseResponse
            } else {
                if (isExpired(now, leaseData)) {
                    return renewAlreadyExpiredLease(now, leaseData, leaseRequest)
                }
                return declineLease(now, leaseData)
            }
        } finally {
            lock.unlock()
        }
    }

    private fun grantNewLease(now: Long, leaseRequest: LeaseCreationRequest): Pair<LeaseData, LeaseCreationResponse> {
        val newLeaseData = LeaseData()
        newLeaseData.leaseName = leaseRequest.leaseName
        newLeaseData.startTime = now
        newLeaseData.durationInMillis = leaseRequest.durationInMillis
        newLeaseData.token = UUID.randomUUID().toString()

        val leaseResponse = LeaseCreationResponse()
        leaseResponse.leaseName = leaseRequest.leaseName
        leaseResponse.leaseFound = true
        leaseResponse.granted = true
        leaseResponse.grantedDurationInMillis = leaseRequest.durationInMillis
        leaseResponse.durationTillExpiry = leaseRequest.durationInMillis
        leaseResponse.token = newLeaseData.token

        return newLeaseData to leaseResponse
    }

    private fun renewAlreadyExpiredLease(now: Long, leaseData: LeaseData, leaseRequest: LeaseCreationRequest): LeaseCreationResponse {
        LoggerFactory.logger.info("renewAlreadyExpiredLease: ${leaseData.leaseName}")
        val existingLeaseData : LeaseData = leaseData
        existingLeaseData.startTime = now
        existingLeaseData.durationInMillis = leaseData.durationInMillis
        existingLeaseData.token = UUID.randomUUID().toString()

        val leaseResponse = LeaseCreationResponse()
        leaseResponse.leaseName = leaseData.leaseName
        leaseResponse.leaseFound = true
        leaseResponse.granted = true
        leaseResponse.grantedDurationInMillis = leaseRequest.durationInMillis
        leaseResponse.durationTillExpiry = leaseRequest.durationInMillis
        leaseResponse.token = existingLeaseData.token

        return leaseResponse
    }

    private fun declineLease(now: Long, leaseData: LeaseData): LeaseCreationResponse {
        LoggerFactory.logger.info("declineLease: ${leaseData.leaseName}")
        val declinedLease = LeaseCreationResponse()

        declinedLease.leaseName = leaseData.leaseName
        declinedLease.granted = false
        declinedLease.leaseFound = true
        declinedLease.grantedDurationInMillis = 0
        declinedLease.durationTillExpiry = leaseData.startTime + leaseData.durationInMillis - now

        return declinedLease
    }

    fun durationTillExpiry(now: Long, leaseName: String): LeaseExpiryResponse {
        val lock = locks.computeIfAbsent(leaseName) { ReentrantLock() }

        try {
            lock.lock()
            val leaseData = leases[leaseName]
            if (leaseData == null) {
                return LeaseExpiryResponse(leaseName, "",false, 0)
            } else {
                val durationTillExpiry = (leaseData.startTime + leaseData.durationInMillis - now).max(0L)
                return LeaseExpiryResponse(leaseName, "", true, durationTillExpiry)
            }
        } finally {
            lock.unlock()
        }
    }

    fun renewLease(leaseRequest: LeaseRenewRequest): LeaseRenewResponse {
        val now = System.currentTimeMillis()
        val lock = locks.computeIfAbsent(leaseRequest.leaseName) { ReentrantLock() }

        try {
            lock.lock()
            val leaseData = leases[leaseRequest.leaseName]
            if (leaseData == null) {
                return LeaseRenewResponse(
                    leaseRequest.leaseName, "",false, false, false,0)
            } else {
                if (leaseData.token != leaseRequest.token) {
                    return LeaseRenewResponse(
                        leaseRequest.leaseName, "",true, false, true, 0)
                }
                var durationTillExpiry = leaseData.startTime + leaseData.durationInMillis - now
                if (durationTillExpiry > leaseRequest.durationInMillis) {
                    // Existing lease duration longer than the newly requested lease duration. Leave lease duration the way it is.
                    return LeaseRenewResponse(
                        leaseRequest.leaseName, "",true, true, false, durationTillExpiry)
                }

                if(durationTillExpiry < leaseRequest.durationInMillis) {
                    durationTillExpiry = leaseRequest.durationInMillis
                    leaseData.startTime = now
                    leaseData.durationInMillis = leaseRequest.durationInMillis
                }

                return LeaseRenewResponse(
                    leaseRequest.leaseName, "",true, true, false, durationTillExpiry)
            }
        } finally {
            lock.unlock()
        }
    }

    fun releaseLease(leaseRequest: LeaseReleaseRequest): LeaseReleaseResponse? {
        val lock = locks.computeIfAbsent(leaseRequest.leaseName) { ReentrantLock() }

        try {
            lock.lock()
            val leaseData = leases[leaseRequest.leaseName]
            if (leaseData == null) {
                return LeaseReleaseResponse(leaseRequest.leaseName, "",false, false)
            } else {
                if(leaseData.token == leaseRequest.token) {
                    leases.remove(leaseRequest.leaseName)
                    return LeaseReleaseResponse(leaseRequest.leaseName, "", true, true, true)
                }
                return LeaseReleaseResponse(leaseRequest.leaseName, "", true, false, false)
            }
        } finally {
            lock.unlock()
        }
    }

    private fun isExpired(now: Long, leaseData: LeaseData): Boolean {
        return leaseData.startTime + leaseData.durationInMillis < now
    }

    fun clear() {
        leases.clear()
    }

}
