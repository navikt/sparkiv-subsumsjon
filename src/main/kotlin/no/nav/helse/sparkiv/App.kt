package no.nav.helse.sparkiv

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.kafka.AivenConfig
import com.github.navikt.tbd_libs.kafka.Config
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.naisful.naisApp
import io.ktor.server.application.ServerReady
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.time.Duration.Companion.seconds

private val defaultConsumerProperties = Properties().apply {
    this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
    this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
}

internal val logger = LoggerFactory.getLogger("no.nav.helse.sparkiv")

fun main() {
    app(System.getenv(), AivenConfig.default)
}

fun app(env: Map<String, String>, kafkaConfig: Config) {
    val factory = ConsumerProducerFactory(kafkaConfig)
    val dataSourceBuilder = DataSourceBuilder(env)
    val kafkaTopic = env.getValue("KAFKA_TOPIC")
    val groupId = env.getValue("CONSUMER_GROUP_ID")
    val consumer = KafkaConsumer(groupId, kafkaTopic, defaultConsumerProperties, factory)
    val app = naisApp(
        meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        objectMapper = jacksonObjectMapper(),
        applicationLogger = logger,
        callLogger = LoggerFactory.getLogger("no.nav.helse.sparkiv.calls"),
        applicationModule = {},
        gracefulShutdownDelay = 10.seconds,
        statusPagesConfig = {},
        preStopHook = consumer::stop,
    )

    app.monitor.subscribe(ServerReady) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            logger.error("Exception caught", throwable)
            app.stop()
        }
        dataSourceBuilder.migrate()
        val scope = CoroutineScope(Dispatchers.Default + exceptionHandler)
        scope.launch {
            consumer.consume(MeldingDao(dataSourceBuilder.getDataSource()))
        }
    }

    app.start(wait = true)
}
