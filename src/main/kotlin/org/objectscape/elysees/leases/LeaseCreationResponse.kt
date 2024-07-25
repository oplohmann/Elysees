package org.objectscape.elysees.leases

class LeaseCreationResponse(
    leaseName: String = "",
    badRequest: String = "",
    leaseFound: Boolean = false,

    var granted: Boolean = false,
    var token: String = "",
    var grantedDurationInMillis: Long = 0,
    var durationTillExpiry: Long = 0

) : LeaseAbstractResponse(leaseName, badRequest, leaseFound)