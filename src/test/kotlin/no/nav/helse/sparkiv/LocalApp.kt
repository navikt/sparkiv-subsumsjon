package no.nav.helse.sparkiv

import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerRecord
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import tools.jackson.module.kotlin.jacksonObjectMapper

private val kafka =
    ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1")).apply {
        withReuse(true)
        start()
    }
private val kafkaConfig =
    LocalKafkaConfig(
        mapOf(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers).toProperties(),
    )
private val factory = ConsumerProducerFactory(kafkaConfig)

fun main() {
    val topic = "topic.v1"
    val scope = CoroutineScope(Dispatchers.Default)
    runBlocking(scope.coroutineContext) {
        logger.info("Starting local app")
        launch {
            app(
                env =
                    database.envvars +
                        mapOf(
                            "KAFKA_TOPIC" to topic,
                            "CONSUMER_GROUP_ID" to "local-consumer",
                            "KAN_SE_SUBSUMSJONER" to "true",
                        ),
                kafkaConfig = kafkaConfig,
            )
        }
        val meldinger = dummyMeldinger()
        factory.createProducer().use { producer ->
            meldinger.forEach { melding ->
                logger.info("Produserer dummy-melding for vedtaksperiodeId=${melding.vedtaksperiodeId} eventName=${melding.eventName}")
                producer.send(ProducerRecord(topic, melding.json))
            }
        }
        logger.info("Ferdig med å produsere dummy-meldinger. Åpne http://localhost:8080 for å søke opp meldinger.")
        logger.info("Eksempel-vedtaksperiodeId-er: ${meldinger.map { it.vedtaksperiodeId }.distinct().joinToString()}")
        logger.info("Eksempel-fødselsnummer: ${meldinger.map { it.fødselsnummer }.distinct().joinToString()}")
    }
}

private data class DummyMelding(
    val vedtaksperiodeId: String,
    val fødselsnummer: String,
    val eventName: String,
    val json: String,
)

private fun dummyMeldinger(): List<DummyMelding> {
    val mapper = jacksonObjectMapper()
    val resource =
        requireNotNull(object {}.javaClass.getResourceAsStream("/personSubsumsjon.json")) {
            "Fant ikke personSubsumsjon.json på classpath (forventet i src/test/resources)"
        }
    val meldinger = resource.use { mapper.readTree(it) }
    return meldinger.values().map { melding ->
        val vedtaksperiodeId = melding["vedtaksperiodeId"]?.asString() ?: "(ingen vedtaksperiodeId)"
        val fødselsnummer = melding["fodselsnummer"].asString()
        val eventName = melding["eventName"].asString()
        DummyMelding(vedtaksperiodeId, fødselsnummer, eventName, mapper.writeValueAsString(melding))
    }
}

private val database =
    object {
        private val postgres =
            PostgreSQLContainer("postgres:17").apply {
                withReuse(true)
                start()

                println("Database localapp: jdbc:postgresql://localhost:$firstMappedPort/test startet opp, credentials: test og test")
            }
        private val jdbcUrl = postgres.jdbcUrl + "&user=${postgres.username}&password=${postgres.password}"
        val envvars = mapOf("DATABASE_JDBC_URL" to jdbcUrl)
    }
