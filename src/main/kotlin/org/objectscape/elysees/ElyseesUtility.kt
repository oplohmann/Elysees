package org.objectscape.elysees

import org.objectscape.elysees.leases.LeaseCreationRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate

open abstract class ElyseesUtility {

    constructor(urlPrefix: String) {
        if(urlPrefix.isEmpty()) {
            throw IllegalArgumentException("urlPrefix must not be empty string")
        }
        this.urlPrefix = urlPrefix
    }

    private var urlPrefix: String = ""

    protected val restTemplate: RestTemplate = RestTemplate()

    protected fun appendToUrl(callName: String) = "$urlPrefix/$callName"

    protected fun <T> submitForEntity(url: String, leaseCreationData: LeaseCreationRequest, javaClass: Class<T>) : ResponseEntity<T> {
        return restTemplate.postForEntity(url, leaseCreationData, javaClass)
    }

}