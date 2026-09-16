package no.nav.helse.sparkiv

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.kafka.AivenConfig
import com.github.navikt.tbd_libs.kafka.Config
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.naisful.naisApp
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ServerReady
import io.ktor.server.http.content.staticResources
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
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
    val meldingDao = MeldingDao(dataSourceBuilder.getDataSource())
    val kafkaTopic = env.getValue("KAFKA_TOPIC")
    val groupId = env.getValue("CONSUMER_GROUP_ID")
    val consumer = KafkaConsumer(groupId, kafkaTopic, defaultConsumerProperties, factory)

    // Toggle for å skru av/på søk på fødselsnummer. Skal kun være "true" i dev inntil løsningen
    // er sikret med autentisering (se KAN_SE_SUBSUMSJONER i deploy/dev.yml og deploy/prod.yml).
    val kanSeSubsumsjoner = env["KAN_SE_SUBSUMSJONER"] == "true"

    val app = naisApp(
        meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
        objectMapper = jacksonObjectMapper(),
        applicationLogger = logger,
        callLogger = LoggerFactory.getLogger("no.nav.helse.sparkiv.calls"),
        applicationModule = {
            routing {
                // Frontendens statiske filer (bygget av frontend/, kopiert inn i static/ av
                // build.gradle.kts sin processResources-task) serveres på samme origin som API-et.
                staticResources("/", "static")
                if (kanSeSubsumsjoner) {
                    get("/vedtaksperiode/{vedtaksperiodeId}") {
                        val vedtaksperiodeId = try {
                            UUID.fromString(call.parameters["vedtaksperiodeId"])
                        } catch (err: IllegalArgumentException) {
                            return@get call.respond(HttpStatusCode.BadRequest, "Ugyldig vedtaksperiodeId")
                        }
                        val meldinger = meldingDao.hentMeldinger(vedtaksperiodeId)
                        if (meldinger.isEmpty()) return@get call.respond(HttpStatusCode.NotFound)
                        call.respondText(
                            meldinger.joinToString(prefix = "[", postfix = "]", separator = ","),
                            ContentType.Application.Json
                        )
                    }
                }
                if (kanSeSubsumsjoner) {
                    get("/fodselsnummer/{fodselsnummer}") {
                        val fødselsnummer = call.parameters["fodselsnummer"]
                        if (fødselsnummer == null || !fødselsnummer.matches(Regex("\\d{11}"))) {
                            return@get call.respond(HttpStatusCode.BadRequest, "Ugyldig fødselsnummer")
                        }
                        val meldinger = meldingDao.hentMeldinger(fødselsnummer)
                        if (meldinger.isEmpty()) return@get call.respond(HttpStatusCode.NotFound)
                        call.respondText(
                            meldinger.joinToString(prefix = "[", postfix = "]", separator = ","),
                            ContentType.Application.Json
                        )
                    }
                }
            }
        },
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
            consumer.consume(meldingDao)
        }
    }

    app.start(wait = true)
}

