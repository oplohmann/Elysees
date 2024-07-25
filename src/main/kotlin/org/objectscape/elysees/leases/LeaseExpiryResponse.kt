package org.objectscape.elysees.leases

class LeaseExpiryResponse(
    leaseName: String = "", badRequest: String = "", leaseFound: Boolean = false, var durationTillExpiry: Long = 0)
    : LeaseAbstractResponse(leaseName, badRequest, leaseFound)
