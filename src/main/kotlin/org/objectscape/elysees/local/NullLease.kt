package org.objectscape.elysees.local

class NullLease : Lease {

    override fun leaseName() = ""
    override fun startTime(): Long = 0
    override fun isGranted() = false
    override fun isExpired(): Boolean = true
    override fun isExpiredInMillis(durationInMillis: Long): Boolean = true
    override fun grantedDurationInMillis() = 0L
    override fun durationTillExpiry() = 0L
    override fun ifGranted(function: () -> Unit) { }
    override fun ifNotGranted(function: () -> Unit) { }
    override fun nullIfGranted(): Boolean? = null
    override fun release() { }
    override fun renew(durationInMillis: Long): Boolean { return false }
    override fun request(durationInMillis: Long, maxTryDurationInMillis: Long): Boolean = false

}