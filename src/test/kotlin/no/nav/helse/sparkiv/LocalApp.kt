package no.nav.helse.sparkiv

import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerRecord
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.ZonedDateTime
import java.util.*

private val kafka = ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1")).apply {
    withReuse(true)
    start()
}
private val kafkaConfig = LocalKafkaConfig(
    mapOf(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG to kafka.bootstrapServers).toProperties()
)
private val factory = ConsumerProducerFactory(kafkaConfig)

fun main() {
    val topic = "topic.v1"
    val scope = CoroutineScope(Dispatchers.Default)
    val message = """{ "id": "${UUID.randomUUID()}", "fodselsnummer": "foobar", "tidsstempel": "${ZonedDateTime.now()}", "eventName": "subsumsjon" }"""
    runBlocking(scope.coroutineContext) {
        logger.info("Starting local app")
        launch { app(env = database.envvars + mapOf("KAFKA_TOPIC" to topic, "CONSUMER_GROUP_ID" to "local-consumer"), kafkaConfig = kafkaConfig) }
        factory.createProducer().use {
            val randomUUID = UUID.randomUUID()
            logger.info("Producing message with id=${randomUUID}")
            it.send(ProducerRecord(topic, message))
        }
    }
}

private val database =
    object {
        private val postgres =
            PostgreSQLContainer<Nothing>("postgres:17").apply {
                withReuse(true)
                start()

                println("Database localapp: jdbc:postgresql://localhost:$firstMappedPort/test startet opp, credentials: test og test")
            }
        private val jdbcUrl = postgres.jdbcUrl + "&user=${postgres.username}&password=${postgres.password}"
        val envvars = mapOf("DATABASE_JDBC_URL" to jdbcUrl)
    }
