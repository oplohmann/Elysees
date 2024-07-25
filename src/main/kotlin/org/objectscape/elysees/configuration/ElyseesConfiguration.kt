package org.objectscape.elysees.configuration

import org.objectscape.elysees.LeaseStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class ElyseesConfiguration {

    @Bean
    fun leaseStore(elyseesProperties: ElyseesProperties): LeaseStore = LeaseStore(elyseesProperties)

}