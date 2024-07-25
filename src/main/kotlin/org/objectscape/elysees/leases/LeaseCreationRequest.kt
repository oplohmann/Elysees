package org.objectscape.elysees.leases

class LeaseCreationRequest(leaseName: String, var durationInMillis: Long) : LeaseCore(leaseName)