package org.objectscape.elysees.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("elysees")
class ElyseesProperties {

    @Value("\${elysees.maxLeaseDuration}")
    val maxLeaseDuration = Long.MAX_VALUE

    @Value("\${elysees.maxLeaseExtensionDuration}")
    val maxLeaseExtensionDuration = Long.MAX_VALUE

    @Value("\${elysees.urlPrefix}")
    val url = "http://localhost:8080/elysees-api/v1"

}