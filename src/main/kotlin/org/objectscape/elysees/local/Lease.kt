package org.objectscape.elysees.local

interface Lease {

    fun leaseName(): String
    fun startTime(): Long
    fun isGranted(): Boolean
    fun isExpired(): Boolean
    fun isExpiredInMillis(durationInMillis: Long): Boolean
    fun grantedDurationInMillis(): Long
    fun durationTillExpiry(): Long
    fun ifGranted(function: () -> Unit)
    fun ifNotGranted(function: () -> Unit)
    fun nullIfGranted(): Boolean?
    fun release()
    fun renew(durationInMillis: Long): Boolean
    fun request(durationInMillis: Long, maxTryDurationInMillis: Long): Boolean

}