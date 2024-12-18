package no.nav.helse.sparkiv

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.kafka.AivenConfig
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.naisful.naisApp
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.slf4j.LoggerFactory
import java.util.*

private val config = AivenConfig.default
private val factory = ConsumerProducerFactory(config)

private const val groupId = "sparkiv-subsumsjon-v1"
private val defaultConsumerProperties = Properties().apply {
    this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
}

internal val logger = LoggerFactory.getLogger("no.nav.helse.sparkiv")

fun main() {
    val consumer = KafkaConsumer(groupId, defaultConsumerProperties, factory)
    val app = naisApp(
        meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        objectMapper = jacksonObjectMapper(),
        applicationLogger = logger,
        callLogger = LoggerFactory.getLogger("no.nav.helse.sparkiv.calls"),
        applicationModule = {},
        statusPagesConfig = {},
        preStopHook = consumer::stop
    )

    app.start(wait = false)
    consumer.consume()
}
