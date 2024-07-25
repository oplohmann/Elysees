package org.objectscape.elysees.leases

class LeaseData(leaseName: String = "", var durationInMillis: Long = 0, var startTime: Long = 0, var token: String = "")
    : LeaseCore(leaseName)