package org.objectscape.elysees

import org.objectscape.elysees.configuration.ElyseesProperties
import org.objectscape.elysees.utils.LoggerFactory.Companion.logger
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication


@EnableConfigurationProperties(ElyseesProperties::class)
@SpringBootApplication
class ElyseesApplication

fun main(args: Array<String>) {
    logger.info("Starting up ElyseesApplication")
    runApplication<ElyseesApplication>(*args)
}