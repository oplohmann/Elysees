package org.objectscape.elysees.leases

class LeaseReleaseResponse(
    leaseName: String = "",
    badRequest: String = "",
    leaseFound: Boolean = false,
    var releaseSuccessful: Boolean = false,
    var leaseOwner: Boolean = false) : LeaseAbstractResponse(leaseName, badRequest, leaseFound)