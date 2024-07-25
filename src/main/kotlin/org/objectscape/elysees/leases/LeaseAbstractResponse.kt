package org.objectscape.elysees.leases

open abstract class LeaseAbstractResponse(var leaseName: String = "", var badRequest: String = "", var leaseFound: Boolean = false)