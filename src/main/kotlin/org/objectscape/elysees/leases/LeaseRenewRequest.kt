package org.objectscape.elysees.leases

class LeaseRenewRequest(leaseName: String = "", var durationInMillis: Long = 0, var token: String = "")
    : LeaseCore(leaseName)