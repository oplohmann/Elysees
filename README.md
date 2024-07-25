# Elysees

Elysees is a liltle framework written in Kotlin providing remote leases. A lease is a timely limited reservation for a shared resource. Leases are an alternative for  locks and have advantages in a distributed setting where locks may not be unlocked due to network failures leaving access to the shared resources being locked up till the system is restarted. Since leases hold a reservation for a resource for limited time only lockup in the event of failures is only for a limited time. See also the article about [Leases](https://en.wikipedia.org/wiki/Lease_(computer_science)) on Wikipedia.

Leases are used in distributed systems such as Kubernetes (see [article](https://kubernetes.io/docs/concepts/architecture/leases/) about leases in Kubernetes), Gigaspaces (see [article](https://docs.gigaspaces.com/latest/dev-java/leases-automatic-expiration.html) about leases Gigaspaces), Jini (see [article](https://river.apache.org/release-doc/3.0.0/specs/html/lease-spec.html) about leases in Jini), and others.

### Implementation

Elysees is written in Kotlin using Spring Boot with the remote lease server being accessed through REST calls from the user client side. For the client the REST calls are hidden through an interface in plain Kotlin. The name Elysees is a worlplay mixing leases and Élysée (champs élysées and Élysée palace in Paris) together as Élysée sounds a bit like lease.

### Sample Code

Some sample code of how to program with leases in Elysees can be seen from the demonstration test case [ClientSideUserTest](https://github.com/oplohmann/Elysees/blob/main/src/test/kotlin/org/objectscape/elysees/ClientSideUserTest.kt). Here is some sample code to show how to program with Elysees:


#### Request and renew a lease

```
val sharedDummyNetworkResource = HashMap<String, Int>()
val leaseDuration = TimeUnit.SECONDS.toMillis(10)
val leaseName = "SOME_SHARED_NETWORK_RESOURCE"

// Ask for a lease that is reserved for us for 10 seconds
val lease: Lease = elysees.requestLease(leaseName, leaseDuration)
lease.nullIfGranted()?.let {
    // This is some demonstration code only where we know that the lease was obtained as there
    // is no other requester for that lease. On the following lines it is assumed that the
    // lease was obtained.
    return
}

// There is no problem with context switches leading to race conditions on this line and the
// following lines below. This is because the shared resource for reserved for much more
// time that would be sufficient to carry out the required operations. In this way leases
// are lot simpler than locks. What on the other hand has to be taken well care of with
// leases is to make sure that the lease has not expired in the meantime.

lease.ifGranted {
    sharedDummyNetworkResource.put("foo", RANDOMIZER.nextInt(1_000))
}

var visitCount = 0
var visitMaxCount = 100
val renewLeaseDuration = TimeUnit.SECONDS.toMillis(1)

while(visitCount < visitMaxCount) {
    visitCount++
    if (lease.renew(renewLeaseDuration)) {
        sharedDummyNetworkResource.put("foo", getSomeNewValue())
        // do some work
    } else {
    // Assuming that renewing the lease was not possible as some other requester obtained it
    // in the meanwhile. Therefore, the lease has to be requested again from the beginning.
    // The lease is requested for <leaseDuration> for a retry period of <leaseDuration * 2>
    if(!lease.request(leaseDuration, leaseDuration * 2)) {
        break // for this simple demonstration code case we just give up and exit
    }
  }
}

```


#### Lease was not obtained in the first place

```
val leaseDuration = TimeUnit.SECONDS.toMillis(2)
val leaseName = "SOME_SHARED_NETWORK_RESOURCE"
val otherLease: Lease = elysees.requestLease(leaseName, leaseDuration)
assertTrue(otherLease.isGranted())

var leaseWasFinallyObtained = false

val lease: Lease = elysees.requestLease(leaseName, leaseDuration)
lease.ifNotGranted {
    // Lease <lease> not obtained as <otherLease> obtained it first and we have to
    // wait till its lease duration has expired or was returned by its own before
    // the end of the lease time. In this simple demonstration code we just wait
    // for <lease.durationTillExpiry()> ms.
    doSomeOtherWorkForDuration(lease.durationTillExpiry())
    if(lease.request(leaseDuration, leaseDuration * 2)) {
        doSomeWorkForLeaseDuration()
    }
    // It is not required to release a lease after access to some shared resource has
    // finished, but it improves throughput as some other thread trying to obtain that
    // lease will be granted access to the shared resource as early as possible.
    lease.release()
}
```
