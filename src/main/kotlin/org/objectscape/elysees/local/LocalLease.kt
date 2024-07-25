package org.objectscape.elysees.local

import org.objectscape.elysees.Elysees
import org.objectscape.elysees.utils.LoggerFactory.Companion.logger

class LocalLease(

    private val elysees: Elysees,
    private val leaseName: String,
    private val token: String,
    private var granted: Boolean,
    private var startTime: Long,
    private var grantedDurationInMillis: Long,
    private var durationTillExpiry: Long) : Lease

{

    override fun leaseName() = leaseName
    override fun startTime() = startTime
    override fun isGranted() = granted
    override fun grantedDurationInMillis() = grantedDurationInMillis

    override fun durationTillExpiry() = durationTillExpiry
    override fun ifGranted(function: () -> Unit) {
        if (granted) {
            function()
        }
    }

    override fun ifNotGranted(function: () -> Unit) {
        if (!granted) {
            function()
        }
    }

    override fun nullIfGranted(): Boolean? {
        if (!granted) {
            return false
        }
        return null
    }

    override fun isExpired(): Boolean {
        val now = System.currentTimeMillis()
        return startTime + durationTillExpiry < now
    }

    override fun isExpiredInMillis(durationInMillis: Long): Boolean {
        val now = System.currentTimeMillis()
        return startTime + durationTillExpiry - now < durationInMillis
    }

    override fun release() {
        elysees.releaseLease(leaseName, token)
    }

    override fun renew(durationInMillis: Long): Boolean {
        if (durationInMillis <= 0) {
            throw IllegalArgumentException("invalid duration $durationInMillis")
        }

        if (!isExpiredInMillis(durationInMillis)) {
            return true
        }

        logger.debug("Renewing lease for $leaseName with token $token for $durationInMillis ms")

        val now = System.currentTimeMillis()
        val grantedRenewedDurationInMillis = elysees.renewLease(leaseName, token, durationInMillis) ?: return false

        startTime = now
        durationTillExpiry = grantedRenewedDurationInMillis
        grantedDurationInMillis = grantedRenewedDurationInMillis

        return true
    }

    override fun request(durationInMillis: Long, maxTryDurationInMillis: Long): Boolean {
        val timeTillMaxRetryExceeded = System.currentTimeMillis() + maxTryDurationInMillis
        while(true) {
            val lease = elysees.requestLease(leaseName, durationInMillis)
            if(lease.isGranted()) {
                granted = true
                startTime = lease.startTime()
                grantedDurationInMillis = lease.grantedDurationInMillis()
                durationTillExpiry = lease.durationTillExpiry()
                return true
            }
            if(System.currentTimeMillis() > timeTillMaxRetryExceeded) {
                return false
            }
        }
    }

}
