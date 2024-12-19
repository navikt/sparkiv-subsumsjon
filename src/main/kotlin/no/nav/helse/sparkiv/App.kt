package no.nav.helse.sparkiv

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.kafka.AivenConfig
import com.github.navikt.tbd_libs.kafka.Config
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.naisful.naisApp
import io.ktor.server.application.ServerReady
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.slf4j.LoggerFactory
import java.util.*

private const val groupId = "sparkiv-subsumsjon-v1"
private val defaultConsumerProperties = Properties().apply {
    this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
}

internal val logger = LoggerFactory.getLogger("no.nav.helse.sparkiv")

fun main() {
    app(System.getenv(), AivenConfig.default)
}

fun app(env: Map<String, String>, kafkaConfig: Config) {
    val factory = ConsumerProducerFactory(kafkaConfig)
    val dataSourceBuilder = DataSourceBuilder(env)
    val kafkaTopic = env.getValue("KAFKA_TOPIC")
    val consumer = KafkaConsumer(groupId, kafkaTopic, defaultConsumerProperties, factory)
    val app = naisApp(
        meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        objectMapper = jacksonObjectMapper(),
        applicationLogger = logger,
        callLogger = LoggerFactory.getLogger("no.nav.helse.sparkiv.calls"),
        applicationModule = {
            monitor.subscribe(ServerReady) {
                dataSourceBuilder.migrate()
                consumer.consume(MeldingDao(dataSourceBuilder.getDataSource()))
            }
        },
        statusPagesConfig = {},
        preStopHook = consumer::stop,
    )

    app.start(wait = true)
}
