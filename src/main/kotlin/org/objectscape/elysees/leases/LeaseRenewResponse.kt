package org.objectscape.elysees.leases

class LeaseRenewResponse(
    leaseName: String = "",
    badRequest: String = "",
    leaseFound: Boolean = false,

    var granted: Boolean = false,
    var heldByOtherLease: Boolean = false,
    var grantedDurationInMillis: Long = 0

) : LeaseAbstractResponse(leaseName, badRequest, leaseFound)