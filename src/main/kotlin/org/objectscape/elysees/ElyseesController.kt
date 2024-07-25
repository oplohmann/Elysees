package org.objectscape.elysees

import org.objectscape.elysees.leases.*
import org.objectscape.elysees.utils.LoggerFactory.Companion.logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.util.*


@RestController
@RequestMapping("/")
class ElyseesController {

    @Autowired
    private lateinit var leaseStore : LeaseStore

    @GetMapping("/isalive")
    fun isalive(): String? {
        // http://localhost:8080/elysees-api/v1/isalive
        logger.info("isalive called")
        return LocalDateTime.now().toString()
    }

    @PostMapping("requestLease", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    fun requestLease(@RequestBody leaseRequest: LeaseCreationRequest): ResponseEntity<LeaseCreationResponse> {
        logger.info("called requestLease: ${leaseRequest.leaseName}, ${leaseRequest.durationInMillis} ms")

        validate(leaseRequest)?.let {
            return ResponseEntity.badRequest().body(it)
        }

        return ResponseEntity.ok(leaseStore.requestLease(leaseRequest))
    }

    @PostMapping("renewLease", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    fun renewLease(@RequestBody leaseRequest: LeaseRenewRequest): ResponseEntity<LeaseRenewResponse> {
        logger.info("called renewLease: ${leaseRequest.leaseName}, ${leaseRequest.token}, {leaseRequest.durationInMillis} ms")

        validate(leaseRequest)?.let {
            return ResponseEntity.badRequest().body(it)
        }

        return ResponseEntity.ok(leaseStore.renewLease(leaseRequest))
    }

    @PostMapping("releaseLease", consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    fun releaseLease(@RequestBody leaseRequest: LeaseReleaseRequest): ResponseEntity<LeaseReleaseResponse> {
        logger.info("called requestLease: ${leaseRequest.leaseName}, ${leaseRequest.token}")

        validate(leaseRequest)?.let {
            return ResponseEntity.badRequest().body(it)
        }

        return ResponseEntity.ok(leaseStore.releaseLease(leaseRequest))
    }

    @GetMapping("/durationTillExpiry", produces = [APPLICATION_JSON_VALUE])
    fun durationTillExpiry(@RequestParam leaseName: String): ResponseEntity<LeaseExpiryResponse> {
        logger.info("durationTillExpiry called")
        if (leaseName.isEmpty()) {
            val leaseExpiryResponse = LeaseExpiryResponse()
            leaseExpiryResponse.badRequest = "lease name must not be empty"
            leaseExpiryResponse.leaseFound = false
            return ResponseEntity.badRequest().body(leaseExpiryResponse)
        }

        val now = System.currentTimeMillis()
        return ResponseEntity.ok(leaseStore.durationTillExpiry(now, leaseName))
    }

    private fun validate(leaseRequest: LeaseCreationRequest) : LeaseCreationResponse? {
        val error = StringBuilder()

        if (leaseRequest.leaseName.isEmpty()) {
            error.append("leaseName must nor be null or empty!")
        }

        if (leaseRequest.durationInMillis <= 0) {
            if (error.isNotEmpty()) {
                error.append(" ")
            }
            error.append("durationInMS must not be negative or 0!")
        }

        if (error.isNotEmpty()) {
            val leaseResponse = LeaseCreationResponse()
            if(leaseRequest.leaseName.isNotEmpty()) {
                leaseResponse.leaseName = leaseRequest.leaseName
            }
            leaseResponse.badRequest = error.toString()
            return leaseResponse
        }

        return null
    }

    private fun validate(leaseRequest: LeaseReleaseRequest): LeaseReleaseResponse? {
        val errors = StringBuilder()

        if (leaseRequest.leaseName.isEmpty()) {
            errors.append("Lease name must not be empty!")
        }

        if (leaseRequest.token.isEmpty()) {
            if (errors.isNotEmpty()) {
                errors.append(" ")
            }
            errors.append("token must not be empty!")
        }

        if (errors.isNotEmpty()) {
            val leaseReleaseResponse = LeaseReleaseResponse()
            if(leaseRequest.leaseName.isNotEmpty()) {
                leaseReleaseResponse.leaseName = leaseRequest.leaseName
            }
            leaseReleaseResponse.badRequest = errors.toString()
            return leaseReleaseResponse
        }

        return null
    }

    private fun validate(leaseRequest: LeaseRenewRequest): LeaseRenewResponse? {
        val errors = StringBuilder()

        if (leaseRequest.leaseName.isEmpty()) {
            errors.append("Lease name must not be empty!")
        }

        if (leaseRequest.durationInMillis <= 0) {
            if (errors.isNotEmpty()) {
                errors.append(" ")
            }
            errors.append("durationInMillis must not be greater than zero!")
        }

        if (leaseRequest.token.isEmpty()) {
            if (errors.isNotEmpty()) {
                errors.append(" ")
            }
            errors.append("Token name must not be empty!")
        }

        if (errors.isNotEmpty()) {
            val leaseRenewResponse = LeaseRenewResponse()
            if(leaseRequest.leaseName.isNotEmpty()) {
                leaseRenewResponse.leaseName = leaseRequest.leaseName
            }
            leaseRenewResponse.badRequest = errors.toString()
            leaseRenewResponse.granted = false
            return leaseRenewResponse
        }

        return null
    }

}